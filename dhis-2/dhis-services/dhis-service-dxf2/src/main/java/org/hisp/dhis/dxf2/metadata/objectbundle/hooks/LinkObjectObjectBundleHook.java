package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2017, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class LinkObjectObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( schema == null || schema.getLinkObjectProperties().isEmpty() )
        {
            return;
        }

        connectLinkObjects( object, bundle, schema.getLinkObjectProperties() );
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( schema == null || schema.getLinkObjectProperties().isEmpty() )
        {
            return;
        }

        List<Property> properties = schema.getLinkObjectProperties();

        clearLinkObjects( persistedObject, properties );
        connectLinkObjects( object, bundle, properties );
    }

    private <T extends IdentifiableObject> void clearLinkObjects( T object, List<Property> properties )
    {
        for ( Property property : properties )
        {
            if ( property.isCollection() )
            {
                ((Collection<?>) ReflectionUtils.invokeMethod( object, property.getGetterMethod() )).clear();
            }
            else
            {
                ReflectionUtils.invokeMethod( object, property.getSetterMethod(), (Object) null );
            }
        }

        sessionFactory.getCurrentSession().flush();
    }

    private <T extends IdentifiableObject> void connectLinkObjects( T object, ObjectBundle bundle, List<Property> properties )
    {
        for ( Property property : properties )
        {
            if ( property.isCollection() )
            {
                Collection<?> objects = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
                objects.forEach( o -> preheatService.connectReferences( o, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
            }
            else
            {
                Object o = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
                preheatService.connectReferences( o, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            }
        }
    }
}
