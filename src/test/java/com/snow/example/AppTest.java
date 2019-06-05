package com.snow.example;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void testHashSet(){
        List<Long> list = new ArrayList<>();
        list.add(1L);
        list.add(2L);
        list.add(3L);
        Set<Long> storeIdSet = new HashSet<>(list);
        List listExample = new ArrayList(list);
        Set<Long> storeIdInput = new HashSet<>();
        storeIdInput.add(3L);
        storeIdSet.retainAll(storeIdInput);
        listExample.retainAll(storeIdInput);

        System.out.println(list.toString());
        System.out.println(storeIdSet.toString());
        System.out.println(storeIdInput.toString());
        System.out.println(listExample.toString());
    }
}
