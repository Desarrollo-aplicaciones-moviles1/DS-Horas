package com.example.deepsourcehoras.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Base de datos local de la app (SQLite del teléfono).
// Versión 3: cada registro guarda quién lo hizo (usuario_id, usuario).
// Desde la versión 2 los datos vienen del microservicio ms-horas y aquí se guarda una copia,
// con los mismos id que tienen en el servidor.
class DsHorasDbHelper(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_BASE, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREAR_PROYECTOS)
        db.execSQL(CREAR_REGISTROS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Es solo una copia del servidor: se arma de nuevo y se vuelve a sincronizar
        db.execSQL("DROP TABLE IF EXISTS registros")
        db.execSQL("DROP TABLE IF EXISTS proyectos")
        onCreate(db)
    }

    companion object {
        const val NOMBRE_BASE = "ds_horas.db"
        const val VERSION = 3

        private const val CREAR_PROYECTOS = """
            CREATE TABLE proyectos (
                id INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL UNIQUE,
                cliente TEXT NOT NULL,
                progreso REAL NOT NULL DEFAULT 0,
                fijado INTEGER NOT NULL DEFAULT 0,
                fecha_cierre TEXT
            )
        """

        private const val CREAR_REGISTROS = """
            CREATE TABLE registros (
                id INTEGER PRIMARY KEY,
                proyecto_id INTEGER NOT NULL REFERENCES proyectos(id) ON DELETE CASCADE,
                tarea TEXT NOT NULL,
                fecha TEXT NOT NULL,
                minutos INTEGER NOT NULL CHECK (minutos > 0),
                foto TEXT,
                creado_en INTEGER NOT NULL,
                usuario_id INTEGER,
                usuario TEXT
            )
        """
    }
}
