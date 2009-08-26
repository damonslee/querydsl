package com.mysema.query.codegen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang.ClassUtils;

import com.mysema.query.util.TypeUtil;

/**
 * 
 * @author tiwe
 * 
 */
public class TypeModelFactory {

    private final Collection<Class<? extends Annotation>> entityAnnotations;
    
    private final Map<List<Type>, TypeModel> cache = new HashMap<List<Type>, TypeModel>();

    @SuppressWarnings("unchecked")
    public TypeModelFactory(Class<?>... entityAnnotations){
        this.entityAnnotations = (List)Arrays.asList(entityAnnotations);
    }
    
    public TypeModelFactory(List<Class<? extends Annotation>> entityAnnotations){
        this.entityAnnotations = entityAnnotations;
    }
    
    public TypeModel create(Class<?> cl){
        return create(cl, cl);
    }
    
    public TypeModel create(Class<?> cl, Type genericType) {
        List<Type> key = Arrays.<Type> asList(cl, genericType);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }else{
            if (cl.isPrimitive()) {
                cl = ClassUtils.primitiveToWrapper(cl);
            }
            TypeModel value;
            boolean entity= false;
            for (Class<? extends Annotation> clazz : entityAnnotations){
                if (cl.getAnnotation(clazz) != null){
                    entity = true;
                    break;
                }
            }            
            if (entity){
                value = new TypeModel(TypeCategory.ENTITY, cl);
                
            }else if (cl.isArray()) {
                value = createArrayType(create(cl.getComponentType()));

            } else if (cl.isEnum()) {
                value = new TypeModel(TypeCategory.SIMPLE, cl);

            } else if (Map.class.isAssignableFrom(cl)) {
                TypeModel keyInfo = create(TypeUtil.getTypeParameter(genericType, 0));
                TypeModel valueInfo = create(TypeUtil.getTypeParameter(genericType, 1));
                value = createMapType(keyInfo, valueInfo);

            } else if (List.class.isAssignableFrom(cl)) {
                TypeModel valueInfo = create(TypeUtil.getTypeParameter(genericType, 0));
                value = createListType(valueInfo);

            } else if (Collection.class.isAssignableFrom(cl)) {
                TypeModel valueInfo = create(TypeUtil.getTypeParameter(genericType, 0));
                value = createCollectionType(valueInfo);
                
            } else {
                value = new TypeModel(TypeCategory.get(cl.getName()), cl);
            }
            cache.put(key, value);
            return value;
        }
        
    }
    
    public TypeModel createArrayType(TypeModel valueType) {
        return createComposite(null, valueType, TypeCategory.ENTITYCOLLECTION, TypeCategory.SIMPLECOLLECTION);
    }

    public TypeModel createCollectionType(TypeModel valueType) {
        return createComposite(null, valueType, TypeCategory.ENTITYCOLLECTION, TypeCategory.SIMPLECOLLECTION);
    }

    private TypeModel createComposite(@Nullable TypeModel key, TypeModel value, TypeCategory entity, TypeCategory simple) {
        TypeCategory category;
        if (value.getTypeCategory() == TypeCategory.ENTITY) {
            category = entity;
        } else {
            category = simple;
        }
        return new TypeModel(category, value.getName(), value.getPackageName(), value.getSimpleName(), key, value);

    }

    public TypeModel createListType(TypeModel valueType) {
        return createComposite(null, valueType, TypeCategory.ENTITYLIST, TypeCategory.SIMPLELIST);
    }

    public TypeModel createMapType(TypeModel keyType, TypeModel valueType) {
        return createComposite(keyType, valueType, TypeCategory.ENTITYMAP, TypeCategory.SIMPLEMAP);
    }

}
