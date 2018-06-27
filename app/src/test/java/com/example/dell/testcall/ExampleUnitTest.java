package com.example.dell.testcall;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test(){
        try {
            Field field=Student.class.getDeclaredField("name");
            Student student=new Student();
            field.setAccessible(true);
            field.set(student,"jack");
            student.getNane();
            System.out.print(student.getNane()+"-------------");

        }catch (Exception e){

        }

    }
}