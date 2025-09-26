package com.example.warehouseCounter.ui.adapter.generic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp

class GenericAdapter<T>(
    private val resource: Int,
    items: List<T>,
    private val getText: (T) -> String,   // Función para obtener el texto a mostrar
    private val getTextColor: (T) -> Int  // Función para obtener el color del texto
) : ArrayAdapter<T>(WarehouseCounterApp.context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolder
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        // Si convertView es nulo, inflamos y creamos el ViewHolder
        if (convertView == null) {
            viewHolder = ViewHolder(view.findViewById<TextView>(R.id.descriptionTextView)!!)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        // Actualizamos los datos solo si la posición es válida
        getItem(position)?.let { item ->
            viewHolder.textViewName.text = getText(item)
            viewHolder.textViewName.setTextColor(getTextColor(item))
        }

        return view
    }

    // ViewHolder generalizado
    internal class ViewHolder(val textViewName: TextView)
}
