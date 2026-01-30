/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.lang.reflect;

/**
 *
 */
public interface TypeVariable<D> extends Type {
    Type[] getBounds();

    D getGenericDeclaration();

    String getName();
}
