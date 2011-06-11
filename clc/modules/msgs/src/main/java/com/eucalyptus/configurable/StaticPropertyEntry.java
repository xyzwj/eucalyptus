/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;

public class StaticPropertyEntry extends AbstractConfigurableProperty {
  static Logger LOG = Logger.getLogger( StaticPropertyEntry.class );
  private Field         field;
  public StaticPropertyEntry( Class definingClass, String entrySetName, Field field, String description, String defaultValue, PropertyTypeParser typeParser, Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias, PropertyChangeListener changeListener ) {
    super( definingClass, entrySetName, field, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias, changeListener );
    this.field = field;
  }
  public Field getField( ) {
    return this.field;
  }
  @Override
  public String getValue( ) {
    try {
      return ""+this.field.get( null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      return super.getDefaultValue();
    }
  }
  @Override
  public String setValue( String s ) {
    try {
      Object o = super.getTypeParser( ).parse( s );
      this.fireChange( s );
      this.field.set( null, o );
      LOG.info( "--> Set property value:  " + super.getQualifiedName( ) + " to " + s );
    } catch ( Throwable t ) {
      LOG.warn( "Failed to set property: " + super.getQualifiedName( ) + " because of " + t.getMessage( ) );
      LOG.debug( t, t );
    }
    return this.getValue( );
  }

  public static class StaticPropertyBuilder implements ConfigurablePropertyBuilder {
    private static String qualifiedName( Class c, Field f ) {
      ConfigurableClass annote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
      return annote.root( ) + "." + f.getName( ).toLowerCase( );
    }

    @Override
    public ConfigurableProperty buildProperty( Class c, Field field ) throws ConfigurablePropertyException {
      if( c.isAnnotationPresent( ConfigurableClass.class ) && field.isAnnotationPresent( ConfigurableField.class ) ) {
        ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
        ConfigurableField annote = ( ConfigurableField ) field.getAnnotation( ConfigurableField.class );
        String description = annote.description( );
        String defaultValue = annote.initial( );
        String fq = qualifiedName( c, field );
        String fqPrefix = fq.replaceAll( "\\..*", "" );
        String alias = classAnnote.alias();
        PropertyTypeParser p = PropertyTypeParser.get( field.getType( ) );
        ConfigurableProperty entry = null;
        Class<? extends PropertyChangeListener> changeListenerClass = annote.changeListener( );
        PropertyChangeListener changeListener;
        if( !changeListenerClass.equals( NoopEventListener.class ) ) {
          try {
            changeListener = changeListenerClass.newInstance( );
          } catch ( Throwable e ) {
            changeListener = NoopEventListener.NOOP;
          }          
        } else {
          changeListener = NoopEventListener.NOOP; 
        }
        int modifiers = field.getModifiers( );
        if ( Modifier.isPublic( modifiers ) && Modifier.isStatic( modifiers ) ) {
          entry = new StaticPropertyEntry( c, fqPrefix, field, description, defaultValue, p, annote.readonly( ), annote.displayName(), annote.type(), alias, changeListener );
          entry.setValue( defaultValue );
          return entry;
        } 
      } 
      return null;
    }
  }

  /**
   * @see com.eucalyptus.configurable.AbstractConfigurableProperty#getQueryObject()
   */
  @Override
  protected Object getQueryObject( ) throws Exception {
    return null;
  }
}
