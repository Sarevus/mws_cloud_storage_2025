package com.MWS.handlers;

public class Files {
    public static Object getList(spark.Request request, spark.Response response){
        System.out.println(request.params());

        return "<li><ol>file1</ol><ol>file2</ol><ol>file1</ol><ol>file3</ol><ol>file1</ol></li>";
    }

    public static Object downloadFile(spark.Request request, spark.Response response){

        return "<li><ol>file1</ol><ol>file2</ol><ol>file1</ol><ol>file3</ol><ol>file1</ol></li>";
    }

    public static Object uploadFile(spark.Request request, spark.Response response){

        return "<li><ol>file1</ol><ol>file2</ol><ol>file1</ol><ol>file3</ol><ol>file1</ol></li>";
    }

    public static Object deleteFile(spark.Request request, spark.Response response){

        return "<li><ol>file1</ol><ol>file2</ol><ol>file1</ol><ol>file3</ol><ol>file1</ol></li>";
    }
}
