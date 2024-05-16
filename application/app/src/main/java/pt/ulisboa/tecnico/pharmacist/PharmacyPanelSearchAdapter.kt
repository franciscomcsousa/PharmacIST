package pt.ulisboa.tecnico.pharmacist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PharmacyPanelSearchAdapter(private val medicineList: List<MedicineViewModel>) : RecyclerView.Adapter<PharmacyPanelSearchAdapter.ViewHolder>() {

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

    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageview)
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

}