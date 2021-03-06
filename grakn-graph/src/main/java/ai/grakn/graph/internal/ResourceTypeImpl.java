/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.graph.internal;

import ai.grakn.concept.Resource;
import ai.grakn.concept.ResourceType;
import ai.grakn.exception.GraphOperationException;
import ai.grakn.util.ErrorMessage;
import ai.grakn.util.Schema;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 *     An ontological element which models and categorises the various {@link Resource} in the graph.
 * </p>
 *
 * <p>
 *     This ontological element behaves similarly to {@link ai.grakn.concept.Type} when defining how it relates to other
 *     types. It has two additional functions to be aware of:
 *     1. It has a {@link ai.grakn.concept.ResourceType.DataType} constraining the data types of the values it's instances may take.
 *     2. Any of it's instances are unique to the type.
 *     For example if you have a ResourceType modelling month throughout the year there can only be one January.
 * </p>
 *
 * @author fppt
 *
 * @param <D> The data type of this resource type.
 *           Supported Types include: {@link String}, {@link Long}, {@link Double}, and {@link Boolean}
 */
class ResourceTypeImpl<D> extends TypeImpl<ResourceType<D>, Resource<D>> implements ResourceType<D> {
    ResourceTypeImpl(VertexElement vertexElement) {
        super(vertexElement);
    }

    ResourceTypeImpl(VertexElement vertexElement, ResourceType<D> type, DataType<D> dataType) {
        super(vertexElement, type);
        vertex().propertyImmutable(Schema.VertexProperty.DATA_TYPE, dataType, getDataType(), DataType::getName);
    }

    /**
     * This method is overridden so that we can check that the regex of the new super type (if it has a regex)
     * can be applied to all the existing instances.
     */
    @Override
    public ResourceType<D> sup(ResourceType<D> superType){
        ((ResourceTypeImpl<D>) superType).superSet().forEach(st -> checkInstancesMatchRegex(st.getRegex()));
        return super.sup(superType);
    }

    /**
     * @param regex The regular expression which instances of this resource must conform to.
     * @return The Resource Type itself.
     */
    @Override
    public ResourceType<D> setRegex(String regex) {
        if(getDataType() == null || !getDataType().equals(DataType.STRING)){
            throw new UnsupportedOperationException(ErrorMessage.REGEX_NOT_STRING.getMessage(getLabel()));
        }

        checkInstancesMatchRegex(regex);

        return property(Schema.VertexProperty.REGEX, regex);
    }

    /**
     * Checks that existing instances match the provided regex.
     *
     * @throws GraphOperationException when an instance does not match the provided regex
     * @param regex The regex to check against
     */
    private void checkInstancesMatchRegex(@Nullable String regex){
        if(regex != null) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher;
            for (Resource<D> resource : instances()) {
                String value = (String) resource.getValue();
                matcher = pattern.matcher(value);
                if(!matcher.matches()){
                    throw GraphOperationException.regexFailure(resource, value, regex);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Resource<D> putResource(D value) {
        Objects.requireNonNull(value);
        return putInstance(Schema.BaseType.RESOURCE,
                () -> getResource(value), (vertex, type) -> vertex().graph().factory().buildResource(vertex, type, value));
    }

    @Override
    public <V> Resource<V> getResource(V value) {
        String index = Schema.generateResourceIndex(getLabel(), value.toString());
        return vertex().graph().getConcept(Schema.VertexProperty.INDEX, index);
    }

    /**
     * @return The data type which instances of this resource must conform to.
     */
    //This unsafe cast is suppressed because at this stage we do not know what the type is when reading from the rootGraph.
    @SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
    @Override
    public DataType<D> getDataType() {
        return (DataType<D>) DataType.SUPPORTED_TYPES.get(vertex().property(Schema.VertexProperty.DATA_TYPE));
    }

    /**
     * @return The regular expression which instances of this resource must conform to.
     */
    @Override
    public String getRegex() {
        return vertex().property(Schema.VertexProperty.REGEX);
    }

}
