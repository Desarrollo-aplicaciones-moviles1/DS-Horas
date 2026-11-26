package com.example.deepsourcehoras.data.local

import android.content.ContentValues
import android.database.Cursor

// Implementación con SQLite (SQLiteOpenHelper). Es la copia local de los datos del microservicio:
// permite ver los proyectos y registros aunque el servidor no responda.
class HorasLocalSqlite(private val helper: DsHorasDbHelper) : HorasLocal {

    // Todos los proyectos con sus minutos registrados. Primero los fijados
    override fun proyectos(): List<ProyectoEntidad> {
        val sql = """
            SELECT p.id, p.nombre, p.cliente, p.progreso, p.fijado, p.fecha_cierre,
                   COALESCE(SUM(r.minutos), 0) AS minutos
            FROM proyectos p
            LEFT JOIN registros r ON r.proyecto_id = p.id
            GROUP BY p.id
            ORDER BY p.fijado DESC, p.id
        """
        val proyectos = mutableListOf<ProyectoEntidad>()
        helper.readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                proyectos.add(
                    ProyectoEntidad(
                        id = cursor.getLong(0),
                        nombre = cursor.getString(1),
                        cliente = cursor.getString(2),
                        progreso = cursor.getFloat(3),
                        fijado = cursor.getInt(4) == 1,
                        fechaCierre = cursor.textoONulo(5),
                        minutos = cursor.getInt(6)
                    )
                )
            }
        }
        return proyectos
    }

    // Los más nuevos primero
    override fun registros(limite: Int?): List<RegistroEntidad> {
        val sql = """
            SELECT r.id, r.proyecto_id, p.nombre, r.tarea, r.fecha, r.minutos, r.foto, r.usuario_id, r.usuario
            FROM registros r
            JOIN proyectos p ON p.id = r.proyecto_id
            ORDER BY r.fecha DESC, r.creado_en DESC
        """ + if (limite != null) " LIMIT $limite" else ""
        val registros = mutableListOf<RegistroEntidad>()
        helper.readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) registros.add(cursor.aRegistro())
        }
        return registros
    }

    override fun registro(id: Long): RegistroEntidad? {
        val sql = """
            SELECT r.id, r.proyecto_id, p.nombre, r.tarea, r.fecha, r.minutos, r.foto, r.usuario_id, r.usuario
            FROM registros r
            JOIN proyectos p ON p.id = r.proyecto_id
            WHERE r.id = ?
        """
        helper.readableDatabase.rawQuery(sql, arrayOf(id.toString())).use { cursor ->
            return if (cursor.moveToFirst()) cursor.aRegistro() else null
        }
    }

    // Minutos ya registrados en un día (para no pasar de 24 horas). Al editar se excluye el mismo registro
    override fun minutosDelDia(fecha: String, excluirId: Long?): Int {
        helper.readableDatabase
            .rawQuery(
                "SELECT COALESCE(SUM(minutos), 0) FROM registros WHERE fecha = ? AND id <> ?",
                arrayOf(fecha, (excluirId ?: -1L).toString())
            )
            .use { cursor ->
                return if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
    }

    // Minutos registrados entre dos fechas (yyyy-MM-dd, ambas incluidas). Lo usa el widget de resumen
    override fun minutosEntre(desde: String, hasta: String): Int {
        helper.readableDatabase
            .rawQuery(
                "SELECT COALESCE(SUM(minutos), 0) FROM registros WHERE fecha BETWEEN ? AND ?",
                arrayOf(desde, hasta)
            )
            .use { cursor ->
                return if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
    }

    // Cuánto lleva cada persona en cada proyecto. Los registros sin autor se juntan como "Sin asignar"
    override fun aportes(): List<AporteEntidad> {
        val sql = """
            SELECT proyecto_id, usuario_id, COALESCE(usuario, 'Sin asignar') AS nombre, SUM(minutos)
            FROM registros
            GROUP BY proyecto_id, usuario_id, nombre
            ORDER BY proyecto_id, SUM(minutos) DESC
        """
        val aportes = mutableListOf<AporteEntidad>()
        helper.readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                aportes.add(
                    AporteEntidad(
                        proyectoId = cursor.getLong(0),
                        usuarioId = if (cursor.isNull(1)) null else cursor.getLong(1),
                        usuario = cursor.getString(2),
                        minutos = cursor.getInt(3)
                    )
                )
            }
        }
        return aportes
    }

    // Sincronización completa: lo local queda igual a lo que hay en el servidor
    override fun reemplazarTodo(proyectos: List<ProyectoLocal>, registros: List<RegistroLocal>) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("registros", null, null)
            db.delete("proyectos", null, null)
            proyectos.forEach { db.insertOrThrow("proyectos", null, it.aValores()) }
            registros.forEach { db.insertOrThrow("registros", null, it.aValores()) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // No se usa REPLACE porque borraría la fila y, por la clave foránea, sus registros
    override fun guardarProyecto(proyecto: ProyectoLocal) {
        val db = helper.writableDatabase
        val valores = proyecto.aValores()
        val cambiadas = db.update("proyectos", valores, "id = ?", arrayOf(proyecto.id.toString()))
        if (cambiadas == 0) db.insertOrThrow("proyectos", null, valores)
    }

    override fun guardarRegistro(registro: RegistroLocal) {
        val db = helper.writableDatabase
        val valores = registro.aValores()
        val cambiadas = db.update("registros", valores, "id = ?", arrayOf(registro.id.toString()))
        if (cambiadas == 0) db.insertOrThrow("registros", null, valores)
    }

    override fun eliminarRegistro(id: Long) {
        helper.writableDatabase.delete("registros", "id = ?", arrayOf(id.toString()))
    }

    private fun Cursor.textoONulo(indice: Int): String? = if (isNull(indice)) null else getString(indice)

    private fun Cursor.aRegistro() = RegistroEntidad(
        id = getLong(0),
        proyectoId = getLong(1),
        proyecto = getString(2),
        tarea = getString(3),
        fecha = getString(4),
        minutos = getInt(5),
        foto = textoONulo(6),
        usuarioId = if (isNull(7)) null else getLong(7),
        usuario = textoONulo(8)
    )

    private fun ProyectoLocal.aValores() = ContentValues().apply {
        put("id", id)
        put("nombre", nombre)
        put("cliente", cliente)
        put("progreso", progreso)
        put("fijado", if (fijado) 1 else 0)
        put("fecha_cierre", fechaCierre)
    }

    private fun RegistroLocal.aValores() = ContentValues().apply {
        put("id", id)
        put("proyecto_id", proyectoId)
        put("tarea", tarea)
        put("fecha", fecha)
        put("minutos", minutos)
        put("foto", foto)
        put("creado_en", creadoEn)
        put("usuario_id", usuarioId)
        put("usuario", usuario)
    }
}
