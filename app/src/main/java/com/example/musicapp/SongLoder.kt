package com.example.musicapp

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

fun loadSongs(context: Context): List<Song> {
    val songList = mutableListOf<Song>()
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION
    )

    val cursor = context.contentResolver.query(collection, projection, null, null, null)

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val title = it.getString(titleColumn)
            val artist = it.getString(artistColumn)
            val duration = it.getLong(durationColumn)
            val uri = ContentUris.withAppendedId(collection, id)

            songList.add(Song(id, title, artist, duration, uri))
        }
    }

    return songList
}
