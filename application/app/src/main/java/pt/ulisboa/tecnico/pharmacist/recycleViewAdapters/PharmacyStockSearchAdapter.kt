package pt.ulisboa.tecnico.pharmacist.recycleViewAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.ulisboa.tecnico.pharmacist.utils.PharmacyStockViewModel
import pt.ulisboa.tecnico.pharmacist.R

class PharmacyStockSearchAdapter(
    val pharmacyStockList: List<PharmacyStockViewModel>
    ) : RecyclerView.Adapter<PharmacyStockSearchAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the medicine_card view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pharmacy_stock_card, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val medicineViewModel = pharmacyStockList[position]

        holder.medicineView.text = medicineViewModel.name
        holder.stockView.text = medicineViewModel.stock.toString()
    }

    override fun getItemCount(): Int {
        return pharmacyStockList.size
    }

    interface RecyclerViewEvent {
        fun onItemClick(position: Int)
    }

    // Holds the views for adding it to image and text
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val medicineView: TextView = itemView.findViewById(R.id.pharmacy_stock_medicine_view)
        val stockView: TextView = itemView.findViewById(R.id.pharmacy_stock_stock_view)
    }

}