# FPGARaspbootin64Client

This is a Java client for the FPGA-based Computer loader.
You can get the FPGA Computer from the:
https://github.com/milanvidakovic/FPGAComputer

This Java program tries to connect to the serial port (given as the first command line parameter) and 
then tries to upload the given machine code file (as the second command line parameter) to the FPGA Computer. It looks like this:

java -jar Raspbootin64Client.jar COM3 C:\Temp\program.bin

Or, you can start this program like this:

java -jar Raspbootin64Client.jar gui

This will start the GUI and then you can do the same, but this time, you can use the built-in terminal to see what is RPI sending, and to type text to be sent via serial back to the RPI.

If the upload was successfull, the loader will start the uploaded program. 

Machine code is created using the fork of the customasm. It can be found here:
https://github.com/milanvidakovic/FPGAcustomasm
 