import barcode
from barcode.writer import ImageWriter

# Function to pad medicine_id to 12 digits
def pad_to_12_digits(medicine_id):
    return str(medicine_id).zfill(11)  #digit n0 12 will be the checksum

# generates 10 barcodes from 1 to 10
medicine_ids = range(1, 11)

barcode_format = 'upca'

for medicine_id in medicine_ids:
    padded_medicine_id = pad_to_12_digits(medicine_id)
    barcode_obj = barcode.get_barcode_class(barcode_format)(padded_medicine_id, writer=ImageWriter())
    
    # Save barcode image
    barcode_filename = f'medicine_{medicine_id}'
    barcode_obj.save(barcode_filename)

print("Barcodes generated successfully.")