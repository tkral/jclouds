/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jclouds.vcloud.director.v1_5.domain;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jclouds.vcloud.director.v1_5.domain.ovf.SectionType;
import org.jclouds.vcloud.director.v1_5.domain.ovf.StartupSection;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;


/**
 * Represents the parameters for capturing a vApp to a vApp template.
 * <p/>
 * <p/>
 * <p>Java class for CaptureVAppParams complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="CaptureVAppParams">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.vmware.com/vcloud/v1.5}ParamsType">
 *       &lt;sequence>
 *         &lt;element name="Source" type="{http://www.vmware.com/vcloud/v1.5}ReferenceType"/>
 *         &lt;element ref="{http://schemas.dmtf.org/ovf/envelope/1}Section" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlType(name = "CaptureVAppParams", propOrder = {
      "source",
      "sections"
})
@XmlRootElement(name = "CaptureVAppParams")
public class CaptureVAppParams extends ParamsType {

   public static Builder<?> builder() {
      return new ConcreteBuilder();
   }

   @Override
   public Builder<?> toBuilder() {
      return builder().fromCaptureVAppParams(this);
   }

   private static class ConcreteBuilder extends Builder<ConcreteBuilder> {
   }
   
   public static abstract class Builder<B extends Builder<B>> extends ParamsType.Builder<B> {

      private Reference source;
      private Set<? extends SectionType> sections = Sets.newLinkedHashSet();

      /**
       * @see CaptureVAppParams#getSource()
       */
      public B source(Reference source) {
         this.source = source;
         return self();
      }

      /**
       * Sets source to a new Reference that uses this URI as the href.
       * 
       * @see CaptureVAppParams#getSource()
       */
      public B source(URI source) {
         this.source = Reference.builder().href(source).build();
         return self();
      }
      
      /**
       * @see CaptureVAppParams#getSections()
       */
      public B sections(Set<? extends SectionType> sections) {
         this.sections = checkNotNull(sections, "sections");
         return self();
      }

      public CaptureVAppParams build() {
         return new CaptureVAppParams(this);
      }

      public B fromCaptureVAppParams(CaptureVAppParams in) {
         return fromParamsType(in)
               .source(in.getSource())
               .sections(in.getSections());
      }
   }

   private CaptureVAppParams(Builder<?> builder) {
      super(builder);
      this.source = builder.source;
      this.sections = builder.sections;
   }

   private CaptureVAppParams() {
      // for JAXB
   }

   private CaptureVAppParams(Set<? extends SectionType> sections) {
      this.sections = ImmutableSet.copyOf(sections);
   }

   @XmlElement(name = "Source", required = true)
   protected Reference source;
   @XmlElementRef
   protected Set<? extends SectionType> sections = Sets.newLinkedHashSet();

   /**
    * Gets the value of the source property.
    *
    * @return possible object is
    *         {@link Reference }
    */
   public Reference getSource() {
      return source;
   }

   /**
    * An ovf:Section to configure the captured vAppTemplate.
    *
    *  Gets the value of the section property.
    *    
    * Objects of the following type(s) are allowed in the list
    * {@link SectionType }
    * {@link VirtualHardwareSection }
    * {@link LeaseSettingsSection }
    * {@link EulaSection }
    * {@link RuntimeInfoSection }
    * {@link AnnotationSection }
    * {@link DeploymentOptionSection }
    * {@link StartupSection }
    * {@link ResourceAllocationSection }
    * {@link NetworkConnectionSection }
    * {@link CustomizationSection }
    * {@link ProductSection }
    * {@link GuestCustomizationSection }
    * {@link OperatingSystemSection }
    * {@link NetworkConfigSection }
    * {@link NetworkSection }
    * {@link DiskSection }
    * {@link InstallSection }
    */
   public Set<? extends SectionType> getSections() {
      return Collections.unmodifiableSet(this.sections);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      CaptureVAppParams that = CaptureVAppParams.class.cast(o);
      return equal(source, that.source) &&
            equal(sections, that.sections);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(source,
            sections);
   }

   @Override
   public String toString() {
      return Objects.toStringHelper("")
            .add("source", source)
            .add("sections", sections).toString();
   }

}
