package pt.ulisboa.tecnico.pharmacist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MedicineBarcodeAdapter(
    val medicines: MutableList<MedicineStock>,
    private val listener: RecyclerViewEvent
) : RecyclerView.Adapter<MedicineBarcodeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.medicine_stock_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medicine = medicines[position]
        holder.bind(medicine)
    }


    override fun getItemCount(): Int {
        return medicines.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)/*, View.OnClickListener*/ {
        private val medicineTextView: TextView = itemView.findViewById(R.id.medicine_text_view)
        private val stockTextView: TextView = itemView.findViewById(R.id.medicine_stock_text_view)
        private val buttonIncrease: Button = itemView.findViewById(R.id.button_increase)
        private val buttonDecrease: Button = itemView.findViewById(R.id.button_decrease)

        init {
            //itemView.setOnClickListener(this)
            buttonIncrease.setOnClickListener { updateStock(adapterPosition, 1) }
            buttonDecrease.setOnClickListener { updateStock(adapterPosition, -1) }
        }

        fun bind(medicine: MedicineStock) {
            // Bind medicine data to views
            medicineTextView.text = medicine.name
            stockTextView.text = medicine.stock.toString()
        }

    }

    private fun updateStock(position: Int, delta: Int) {
        val medicine = medicines[position]
        val newStock = (medicine.stock + delta).coerceAtLeast(0)
        if (newStock != medicine.stock) {
            // if zero remove it from the cards!
            if (newStock == 0) {
                medicines.removeAt(position)
                notifyItemRemoved(position)
                listener.onStockEmpty()
            } else {
                medicine.stock = newStock
                notifyItemChanged(position)
            }
        }
    }


    interface RecyclerViewEvent {
        fun onStockEmpty()
    }

}