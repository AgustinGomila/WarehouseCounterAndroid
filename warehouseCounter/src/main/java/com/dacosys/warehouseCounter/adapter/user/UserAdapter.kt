package com.dacosys.warehouseCounter.adapter.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.R.id.descriptionTextView
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.room.entity.user.User


/**
 * Created by Agustin on 18/01/2017.
 */

class UserAdapter(private var resource: Int, user: List<User>) :
    ArrayAdapter<User>(context(), resource, user) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        var v = convertView

        if (v == null || v.tag == null) {
            val vi = LayoutInflater.from(context)
            v = vi.inflate(this.resource, parent, false)

            holder = ViewHolder()
            holder.textViewName = v.findViewById(descriptionTextView)

            // store the holder with the view.
            v.tag = holder
        } else {
            // we've just avoided calling findViewById() on resource everytime
            // just use the viewHolder
            holder = v.tag as ViewHolder
        }

        if (position >= 0) {
            val user = getItem(position)

            if (user != null) {
                // Font colors
                val dimgray = ResourcesCompat.getColor(context().resources, R.color.dimgray, null)
                val black = ResourcesCompat.getColor(context().resources, R.color.black, null)

                if (resource == R.layout.custom_spinner_dropdown_item && v != null) {
                    holder.textViewName!!.text = user.name

                    when (user.userId) {
                        0L -> holder.textViewName!!.setTextColor(dimgray)
                        else -> holder.textViewName!!.setTextColor(black)
                    }
                }
            }
        }

        return v!!
    }

    // our ViewHolder.
    // caches our TextView
    internal class ViewHolder {
        var textViewName: TextView? = null
    }
}