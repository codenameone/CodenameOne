/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util;

/**
 *
 * @author shannah
 */
public class EnumSet<E> extends AbstractSet<E> {
    private HashSet<E> internal = new HashSet<E>();

    @Override
    public Iterator<E> iterator() {
        return internal.iterator();
    }

    @Override
    public int size() {
        return internal.size();
    }
    
    public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
        throw new UnsupportedOperationException("EnumSet.allOf not implemented yet");
    }
    
}
