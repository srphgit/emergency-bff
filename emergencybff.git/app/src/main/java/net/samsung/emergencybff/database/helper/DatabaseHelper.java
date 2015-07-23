package net.samsung.emergencybff.database.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DBName = "bff_bundle.db";
    private static final int DBVer = 1;

    // TABLES
    private static final String TABLE_NAME = "emergency_list";
    private static final String REGION_TABLE = "region";
    private static final String CITY_TABLE = "cities";

    // FIELDS
    public static final String _ID = "_id";
    public static final String _NAME = "e_name";
    public static final String _ADDRESS = "e_address";
    public static final String _NUMBER = "e_number";
    public static final String _LAT = "e_latitude";
    public static final String _LONG = "e_longitude";
    private static final String _TYPE = "e_type";
    private static final String TAG = "BFF.DatabaseHelper";

    private static final String _CITY_ID = "c_id";
    private static final String _CITY = "c_name";
    private static final String _REGION_ID = "r_id";
    private static final String _REGION = "r_name";

    private static DatabaseHelper _instance;
    private static final String DBPath = "/data/data/net.samsung.emergencybff/databases/";
    private SQLiteDatabase myDB;
    private Context myContext;

    List<JSONObject> mContactList;
    private String[] cities = {
            "Taguig",
            "Pasig",
            "Makati",
            "Manila",
            "Caloocan",
            "Quezon City",
            "Valenzuela",
            "Paranaque",
            "Las Piñas",
            "Muntinlupa",
            "Mandaluyong",
            "Pasay",
            "Navotas",
            "San Juan",
            "Marikina",
            "Malabon",
            "Pateros"
    };

    /**
     * @param context
     * @param name
     * @param version
     */
    public DatabaseHelper(Context context, String name, int version) {
        super(context, name, null, version);
        this.myContext = context;
    }

    public static void createInstance(Context context) {
        if (_instance == null)
            _instance = new DatabaseHelper(context, DBName, DBVer);
    }

    public static DatabaseHelper getInstance() {
        if (_instance == null)
            throw new SQLException("Create database instance first.");
        else
            return _instance;
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();

        if (!dbExist) {
            //By calling this method, an empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            Log.e(TAG, this.getDatabaseName());
            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }

    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     *
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase() {

        SQLiteDatabase checkDB = null;

        try {
            String myPath = DBPath + DBName;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            Log.d(TAG, "Database doesn't exist yet. Creating a new database");
            //database doesn't exist yet.
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    private void copyDataBase() throws IOException {

        //Open your local db as the input stream
        Log.e(TAG, "Open assets");

        String[] files = myContext.getAssets().list("");
        for (String i : files)
            Log.e(TAG, "Asset: " + i);
        InputStream myInput = myContext.getAssets().open(DBName);
        Log.e(TAG, "Open assets success");
        // Path to the just created empty db
        String outFileName = DBPath + DBName;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);
        Log.e(TAG, "Open DBPath");
        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        Log.e(TAG, "Write Success");
        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    public void openDataBase() throws SQLException {
        //Open the database
        String myPath = DBPath + DBName;
        myDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close() {
        if (myDB != null)
            myDB.close();
        super.close();
    }

    public Cursor getInfo(int type) {
        this.close();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query for entries with complete data only
        return db.rawQuery(
                "select " +
                        _ID + "," +
                        _NUMBER + "," +
                        _LAT + "," +
                        _LONG +
                        " from " + TABLE_NAME +
                        " where " + _TYPE + "=" + type +
                        " and " + _NUMBER + "!=''" +
                        " and " + _LAT + "!='0'" +
                        " and " + _LONG + "!='0'"
                , null);
    }

    public Cursor getInfoFromCity(int type, int cityId) {
        this.close();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query for entries with complete data only
        return db.rawQuery(
                "select " +
                        _ID + "," +
                        _NUMBER + "," +
                        _LAT + "," +
                        _LONG +
                        " from " + TABLE_NAME +
                        " where " + _TYPE + "=" + type +
                        " and " + _CITY_ID + "=" + cityId +
                        " and " + _NUMBER + "!=''" +
                        " and " + _LAT + "!='0'" +
                        " and " + _LONG + "!='0'"
                , null);
    }

    public Cursor getCities(CharSequence entry) {
        Log.d(TAG, "Get cities with entry: " + entry);
        this.close();
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("select " +
                "c." + _ID + "," +
                "r." + _REGION + "," +
                "c." + _CITY + "," +
                "c." + _LAT + "," +
                "c." + _LONG + " from " +
                CITY_TABLE + " c, " +
                REGION_TABLE + " r " +
                "where r." + _ID + "=" + "c." + _REGION_ID +
                "and c." + _CITY + " LIKE '%" + entry.toString() + "%'"
                , null);
    }

    public Cursor getContact(int nearestId) {
        this.close();
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("select " +
                _NAME + "," +
                _ADDRESS + "," +
                _NUMBER + "," +
                _LAT + "," +
                _LONG +
                " from " + TABLE_NAME + " where " + _ID + "=" + nearestId, null);
    }

    public void addContactsIfNotExists(String jsonText){
        this.close();
        openDataBase();
        try {
            populateList(jsonText);
            for(int x = 0 ; x < mContactList.size(); x++){
                String sql = "SELECT * FROM " + TABLE_NAME + " " +
                        "WHERE " + _LAT+ " = "+ mContactList.get(x).getDouble("latitude") +" " +
                        "AND "+ _LONG +" = "+ mContactList.get(x).getDouble("longitude") +" AND "+ _TYPE +" = "+ mContactList.get(x).getInt("type") +"";
                Log.e(TAG, sql);
                Cursor cursor = myDB.rawQuery(sql, null);
                if(cursor == null || !cursor.moveToFirst()){
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(_ID, mContactList.get(x).getInt("ID"));
                    contentValues.put(_ADDRESS, mContactList.get(x).getString("address"));
                    contentValues.put(_CITY_ID, getCityID(mContactList.get(x).getString("city")));
                    contentValues.put(_LAT, mContactList.get(x).getDouble("latitude"));
                    contentValues.put(_LONG, mContactList.get(x).getDouble("longitude"));
                    contentValues.put(_NAME, mContactList.get(x).getString("name"));
                    contentValues.put(_NUMBER, mContactList.get(x).getString("number"));
                    contentValues.put(_TYPE, mContactList.get(x).getInt("type"));
                    long rowInserted = myDB.insert(TABLE_NAME, null, contentValues);
                    if(rowInserted != -1)
                        Log.e(TAG, "ADDED TO DB: " + rowInserted);
                    else
                        Log.e(TAG, "SOMETHING WENT WRONG!");

                } else {
                    Log.e(TAG, "NOT ADDED TO DB: " + cursor.getInt(0));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //add insertion here
    }

    private int getCityID(String city) {
        for(int x = 0 ; x < cities.length; x++){
            if(cities[x].toLowerCase().contains(city.toLowerCase())){
                return (x + 1);
            }
        }
        return 0;
    }

    private void populateList(String s) throws JSONException {
        JSONObject list = new JSONObject(s);
        mContactList = new ArrayList<>();

        String[] types = {"hospital", "police", "firedept", "ambulance"};
        for(int c = 0 ; c < types.length; c++){
            JSONArray array = list.getJSONArray(types[c]);
            for (int x = 0 ; x < array.length(); x++) {
                try {
                    mContactList.add(array.getJSONObject(x));
                } catch (Exception ex){continue;}
            }
        }
    }

    // TEST CODE

        /*        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + _NAME + " TEXT NOT NULL,"
                + _ADDRESS + " TEXT,"
                + _CITY + " TEXT NOT NULL,"
                + _NUMBER + " TEXT NOT NULL,"
                + _LAT + " REAL NOT NULL,"
                + _LONG + " REAL NOT NULL,"
                + _TYPE + " INTEGER NOT NULL"
                + ")";
                db.execSQL(CREATE_TABLE);*/

}
