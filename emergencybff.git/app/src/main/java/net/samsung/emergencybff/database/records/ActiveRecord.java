package net.samsung.emergencybff.database.records;

import java.util.ArrayList;

import org.json.JSONException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import net.samsung.emergencybff.database.helper.DatabaseHelper;

public abstract class ActiveRecord<E> {

	private DatabaseHelper mHelper;
	
	public ActiveRecord( Context context ) {
		mHelper = DatabaseHelper.getInstance();
	}
	
	protected SQLiteDatabase getWritableDatabase() {
		return mHelper.getWritableDatabase();
	}
	
	protected SQLiteDatabase getReadableDatabase() {
		return mHelper.getReadableDatabase();
	}
	
	public abstract long insert(E e);
	public abstract long update();
	public abstract void delete();
	public abstract ArrayList<E> getAll();
	public abstract ArrayList<E> getAll(int offset , int limit);
	public abstract ArrayList<E> get(String args , String... argValues);
	
	public static String getTableStructure() {
		return null;
	}
	public static String getName() {
		return null;
	};
}

