package com.example.samsung.p0391_sqliteonupgradedb;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = "myLog",
                        DB_NAME = "staff", TABLE_PEOPLE = "people", TABLE_POSITION = "position",
                        CREATE_TABLE = "create table ", ALTER_TABLE = "alter table ",
                        CREATE_TEMPORARY_TABLE = "create temporary table ", INSERT_INTO = "insert into ",
                        SELECT = "select ", DROP_TABLE = "drop table ", PRIMARY_KEY = " primary key ",
                        AUTOINCREMENT = "autoincrement ",
                        FROM = " from ", AS = " as ", ON = " on ",
                        TEXT = " text", INTEGER = " integer",
                        INNER_JOIN = " inner join ", ADD_COLUMN = " add column ";
    private final int DB_VERSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Log.d(LOG_TAG, "--- Staff db v." + database.getVersion() + " ---");
        writeStaff(database);
        database.close();
    }

    //запрос данных и вывод в лог
    private void writeStaff(SQLiteDatabase database) {
        String sqlCommand = SELECT + "*" + FROM + TABLE_PEOPLE;
        Cursor cursor = database.rawQuery(sqlCommand, null);
        logCursor(cursor, "Table " + TABLE_PEOPLE);
        cursor.close();

        sqlCommand = SELECT + "*" +FROM + TABLE_POSITION;
        cursor = database.rawQuery(sqlCommand, null);
        logCursor(cursor, "Table " + TABLE_POSITION);
        cursor.close();

        sqlCommand = SELECT + "PL.name" + AS + "Name, PS.name" + AS + "Position, salary" + AS + "Salary "
                    + FROM + TABLE_PEOPLE + AS +"PL"
                    + INNER_JOIN + TABLE_POSITION + AS + "PS "
                    + ON + "PL.pos_id = PS.id";
        cursor = database.rawQuery(sqlCommand, null);
        logCursor(cursor, TABLE_PEOPLE + INNER_JOIN + TABLE_POSITION);
        cursor.close();
    }

    private void logCursor(Cursor cursor, String s) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Log.d(LOG_TAG, s + ". " + cursor.getCount() + " rows.");
                StringBuilder stringBuilder = new StringBuilder();
                do {
                    stringBuilder.setLength(0);
                    for (String string : cursor.getColumnNames()) {
                        stringBuilder.append(string + " = " + cursor.getString(cursor.getColumnIndex(string)) + "; ");
                    }
                    Log.d(LOG_TAG, stringBuilder.toString());
                } while (cursor.moveToNext());
            }
        } else {
            Log.d(LOG_TAG, s + ". Cursor is null.");
        }
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(LOG_TAG, "--- onCreate database ---");
            String[] people_name = {"Иван", "Марья", "Пётр", "Антон", "Даша", "Борис", "Костя", "Игорь"},
                    pople_position = {"Программист", "Бухгалтер", "Программист", "Программист", "Бухгалтер",
                            "Директор", "Программист", "Охранник"};
            ContentValues contentValues = new ContentValues();

            //создаём таблицу людей
            String sqlCommand = CREATE_TABLE + TABLE_PEOPLE
                    + "(id " + INTEGER + PRIMARY_KEY + AUTOINCREMENT +", "
                    + "name" + TEXT + ", position " + TEXT + ");";
            db.execSQL(sqlCommand);
            //заполняем таблицу
            for (int i = 0; i < people_name.length; i++) {
                contentValues.clear();
                contentValues.put("name", people_name[i]);
                contentValues.put("position", pople_position[i]);
                db.insert(TABLE_PEOPLE, null, contentValues);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(LOG_TAG, "--- onUpgrade database from " + oldVersion + " to " + newVersion + " version ---");

            if (oldVersion == 1 && newVersion == 2) {
                getPositions(db);
            }
        }

        private void getPositions(SQLiteDatabase db) {
            //дданные для таблицы должностей
            ContentValues contentValues = new ContentValues();
            String sqlCommand = SELECT + "position" + AS + "Position" + FROM + TABLE_PEOPLE + AS + "PL"
                    + " group by PL.Position;";
            Cursor cursor = db.rawQuery(sqlCommand, null);
            String[] positions = new String[cursor.getCount()];
            Map<String, Integer> position_salary = new HashMap<>();
            position_salary.put("Директор", 15000);
            position_salary.put("Программист", 13000);
            position_salary.put("Бухгалтер", 15000);
            position_salary.put("Охранник", 15000);

            //создаём таблицу должностей
            sqlCommand = CREATE_TABLE + TABLE_POSITION
                    + "(id " + INTEGER + PRIMARY_KEY + ", name " + TEXT + ", salary " + INTEGER + ");";
            db.execSQL(sqlCommand);

            //заполняем таблицу должностей
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int i = 0, salary = 0;
                    do {
                        for (String string : cursor.getColumnNames()) {
                            positions[i] = cursor.getString(cursor.getColumnIndex(string));
                            contentValues.clear();
                            contentValues.put("id", i);
                            contentValues.put("name", positions[i]);
                            salary = position_salary.containsKey(positions[i]) ? (int) position_salary.get(positions[i]) : 0;
                            contentValues.put("salary", salary);
                            db.insert(TABLE_POSITION, null, contentValues);
                            Log.d(LOG_TAG, "The position \"" + positions[i] + "\" included in table \"" + TABLE_POSITION + "\"");
                        }
                        i++;
                    } while (cursor.moveToNext());
                }
            } else {
                Log.d(LOG_TAG, "Cursor in pretable is null.");
            }
            cursor.close();

            //добавляем к таблице людей колонку с индексами должностей
            db.beginTransaction();
            sqlCommand = ALTER_TABLE + TABLE_PEOPLE + ADD_COLUMN + "pos_id " + INTEGER + ";";
            try {
                db.execSQL(sqlCommand);
                Log.d(LOG_TAG, "--- Adding into table \"" + TABLE_PEOPLE + "\" the column \" pos_id\" ---");
                for (int i = 0; i < positions.length; i++) {
                    contentValues.clear();
                    contentValues.put("pos_id", i);
                    db.update(TABLE_PEOPLE, contentValues, "position = ?", new String[] {positions[i]});
                    Log.d(LOG_TAG, "The index \"" + i + "\" of position \"" + positions[i]
                            + "\" inserted into table \"" + TABLE_PEOPLE + "\"");
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            /**создаём временную таблицу людей для переноса в неё имеющихся данных,
             * поскольку удалить колонку из таблицы нельзя, а можно удалить табилицу
             * только целиком.
             */
            sqlCommand = CREATE_TEMPORARY_TABLE + TABLE_PEOPLE + "_tmp"
                        + "(id " + INTEGER + ", name " + TEXT + ", position " + TEXT + ", pos_id " + INTEGER + ");";
            db.beginTransaction();
            try {
                db.execSQL(sqlCommand);
                db.setTransactionSuccessful();
                Log.d(LOG_TAG, "--- The temporary table \"" + TABLE_PEOPLE + "_tmp\"  has bin created ---");
            } finally {
                db.endTransaction();
            }
            //переносим данные из таблицы people во временную таблицу people_tmp
            sqlCommand = INSERT_INTO + TABLE_PEOPLE + "_tmp " + SELECT + "id, name, position, pos_id " + FROM + TABLE_PEOPLE + ";";
            db.beginTransaction();
            try {
                db.execSQL(sqlCommand);
                db.setTransactionSuccessful();
                Log.d(LOG_TAG, "--- The data from table \"" + TABLE_PEOPLE + "\" into temporary table \""
                        + TABLE_PEOPLE + "_tmp\"  has bin inserted ---");
            } finally {
                db.endTransaction();
            }
            /**удаляем устаревшую версию таблицы people и создаём её новую версию, в которую переносим
             * данные из временной таблицы people_tmp и удаляем её после этого
             */
            db.beginTransaction();
            try {
                sqlCommand = DROP_TABLE + TABLE_PEOPLE + ";";
                db.execSQL(sqlCommand);

                sqlCommand = CREATE_TABLE + TABLE_PEOPLE + "(id " + INTEGER + PRIMARY_KEY + AUTOINCREMENT
                        + ", name " + TEXT + ", pos_id " + INTEGER + ");";
                db.execSQL(sqlCommand);

                sqlCommand = INSERT_INTO + TABLE_PEOPLE + " " + SELECT + "id, name, pos_id " + FROM + TABLE_PEOPLE + "_tmp;";
                db.execSQL(sqlCommand);

                sqlCommand = DROP_TABLE + TABLE_PEOPLE + "_tmp;";
                db.execSQL(sqlCommand);
                db.setTransactionSuccessful();

                Log.d(LOG_TAG, "--- The table \"" + TABLE_PEOPLE + "\" has bin deleted ---");
                Log.d(LOG_TAG, "--- The table \"" + TABLE_PEOPLE + "\" has bin created ---");
                Log.d(LOG_TAG, "--- The data from temporary table \"" + TABLE_PEOPLE + "_tmp\" into table \""
                        + TABLE_PEOPLE + "\"  has bin inserted ---");
                Log.d(LOG_TAG, "--- The temporary table \"" + TABLE_PEOPLE + "_tmp\" has bin deleted ---");
            } finally {
                db.endTransaction();
            }
        }
    }
}
