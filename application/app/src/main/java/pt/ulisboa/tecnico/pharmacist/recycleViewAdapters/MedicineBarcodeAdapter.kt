package pt.ulisboa.tecnico.pharmacist.recycleViewAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.R


class MedicineBarcodeAdapter(
    val medicines: MutableList<MedicineStock>,
    private val listener: RecyclerViewEvent
) : RecyclerView.Adapter<MedicineBarcodeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_medicine_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medicine = medicines[position]
        holder.bind(medicine)
    }


    override fun getItemCount(): Int {
        return medicines.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val medicineTextView: TextView = itemView.findViewById(R.id.medicine_text_view)
        private val stockTextView: TextView = itemView.findViewById(R.id.medicine_stock_text_view)
        private val buttonIncrease: Button = itemView.findViewById(R.id.button_increase)
        private val buttonDecrease: Button = itemView.findViewById(R.id.button_decrease)

        init {
            buttonIncrease.setOnClickListener { updateStock(adapterPosition, 1, itemView.context) }
            buttonDecrease.setOnClickListener { updateStock(adapterPosition, -1, itemView.context) }
        }

        fun bind(medicine: MedicineStock) {
            // Bind medicine data to views
            medicineTextView.text = medicine.name
            stockTextView.text = medicine.stock.toString()
        }

    }

    private fun updateStock(position: Int, delta: Int, context: Context) {
        val medicine = medicines[position]
        val newStock = (medicine.stock + delta).coerceAtLeast(0)
        if (delta > 0 && medicine.maxStock != null && newStock > medicine.maxStock!!) {
            notifyMaxStockReached(medicine.name, context)
        } else {
            if (newStock != medicine.stock) {
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
    }

    private fun notifyMaxStockReached(medicineName: String, context: Context) {
        // TODO - maybe not a TOAST
        Toast.makeText(context, "Max stock reached for $medicineName", Toast.LENGTH_SHORT).show()
    }


    interface RecyclerViewEvent {
        fun onStockEmpty()
    }
}