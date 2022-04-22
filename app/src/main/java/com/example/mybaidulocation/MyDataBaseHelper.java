package com.example.mybaidulocation;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MyDataBaseHelper  extends SQLiteOpenHelper {//1.新建类继承SQLiteOpenHelper

    private Context context;//上下文

    //数据库中创建一张Cell表
    public static final String Cell =
            "CREATE TABLE `cell`  (" +
            "  `num` int(11)," +
            "  `rowid` int(11)," +
            "  `colid` int(11)," +
            "  `time_id` int(11)," +
            "  `speed` float," +
            "  `volume` float ," +
            "  `stopNum` float," +
            "  `label` int(11) )";

    //2.实现构造方法
    public MyDataBaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        //int version-当前数据库的版本号，可用于对数据库进行升级操作
        super(context, name, factory, version);
        this.context = context;
    }


    //3.重写onCreate方法
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(Cell);//执行建表语句，创建数据库
    }

    //4.重写onUpgrade方法
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    // 读取sql文件，插入数据
    public void readSqlFile(SQLiteDatabase db,InputStream in) throws Exception{
        System.out.println("成功读取文件");
        String insertSql = readSQL(in);
        String[] s = insertSql.split(";");
        for(int i = 0;i < s.length;i++){
            if(!TextUtils.isEmpty(s[i])){
                db.execSQL(s[i]);
            }
        }
    }

    // 按行读取sql文件
    private String readSQL(InputStream is) throws Exception{
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer("");
        String str = null;
        while ((str=bufferedReader.readLine()) != null){
            buffer.append(str);
            buffer.append("\n");
        }
        return buffer.toString();
    }
}