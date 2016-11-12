package com.github.gfx.android.orma.example.handwritten

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HandWrittenOpenHelper(context: Context, name: String) : SQLiteOpenHelper(context, name, null, HandWrittenOpenHelper.VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE todo ("
                + "id INTEGER PRIMARY KEY,"
                + "title TEXT NOT NULL,"
                + "content TEXT NULL,"
                + "done BOOLEAN NOT NULL,"
                + "createdTime INTEGER NOT NULL"
                + ")")
        db.execSQL("CREATE INDEX title_on_todo ON todo (title)")
        db.execSQL("CREATE INDEX createdTime_on_todo ON todo (createdTime)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE todo")
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // nop
    }

    companion object {

        internal val VERSION = 4
    }
}
