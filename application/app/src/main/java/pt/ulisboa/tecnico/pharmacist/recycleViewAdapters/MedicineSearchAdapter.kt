package pt.ulisboa.tecnico.pharmacist.recycleViewAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.ulisboa.tecnico.pharmacist.utils.MedicineSearchViewModel
import pt.ulisboa.tecnico.pharmacist.R

class MedicineSearchAdapter(
    val medicineList: List<MedicineSearchViewModel>,
    private val listener: RecyclerViewEvent
    ) : RecyclerView.Adapter<MedicineSearchAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the medicine_card view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.medicine_card, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val medicineViewModel = medicineList[position]

        holder.imageView.setImageResource(medicineViewModel.image)
        holder.textView.text = medicineViewModel.text

    }

    override fun getItemCount(): Int {
        return medicineList.size
    }

    interface RecyclerViewEvent {
        fun onItemClick(position: Int)
    }

    // Holds the views for adding it to image and text
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val imageView: ImageView = itemView.findViewById(R.id.medicine_image_view)
        val textView: TextView = itemView.findViewById(R.id.medicine_text_view)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(adapterPosition)
            }
        }
    }

}