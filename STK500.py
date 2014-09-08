#!/usr/bin/python

#import libraries
import time
import bluetooth as bt

# define a rfcomm bluetooth connection
sock = bt.BluetoothSocket(bt.RFCOMM)
# connect to the bluetooth device
sock.connect(("98:D3:31:B1:CA:BF", 1))

# get in sync with the AVR
for i in range(5):
  print "syncing"
  sock.send('\x30') # STK_GET_SYNC
  sock.send('\x20') # STK_CRC_EOP
  time.sleep(0.05)

# receive sync ack
print "receiving sync ack"
insync = sock.recv(1) # STK_INSYNC
ok = sock.recv(1) # STK_OK

# check received ack
if insync == '\x14' and ok == '\x10':
  print "insync"

# get the MAJOR version of the bootloader
print "getting the MAJOR version of the bootloader"
sock.send('\x41') # STK_GET_PARAMETER
sock.send('\x81') # STK_SW_MAJOR
sock.send('\x20') # SYNC_CRC_EOP
time.sleep(0.05)

# receive bootlader MAJOR version
print "receiving bootloader MAJOR version"
insync = sock.recv(1) # STK_INSYNC
major = sock.recv(1) # STK_SW_MJAOR
ok = sock.recv(1) # STK_OK

# check received sync ack
if insync == '\x14' and ok == '\x10':
  print "insync"

# get the MINOR version of the bootloader
print "getting the MINOR version of the bootloader"
sock.send('\x41') # STK_GET_PARAMETER
sock.send('\x82') # STK_SW_MINOR
sock.send('\x20') # SYNC_CRC_EOP
time.sleep(0.05)

# receive bootlader MINOR version
print "receiving bootloader MINOR version"
insync = sock.recv(1) # STK_INSYNC
minor = sock.recv(1) # STK_SW_MINOR
ok = sock.recv(1) # STK_OK

# check received sync ack
if insync == '\x14' and ok == '\x10':
  print "insync"

print "bootloader version %s.%s" % (ord(major), ord(minor))

# enter programming mode
print "entering programming mode"
sock.send('\x50') # STK_ENTER_PROGMODE
sock.send('\x20') # SYNC_CRC_EOP
time.sleep(0.05)

# receive sync ack
print "receiving sync ack"
insync = sock.recv(1) # STK_INSYNC
ok = sock.recv(1) # STK_OK

# check received sync ack
if insync == '\x14' and ok == '\x10':
  print "insync"

# get device signature
print "getting device signature"
sock.send('\x75') # STK_READ_SIGN
sock.send('\x20') # SYNC_CRC_EOP

# receive device signature
print "receiving device signature"
insync = sock.recv(1) # STK_INSYNC
signature = sock.recv(3) # device
ok = sock.recv(1) # STK_OK

# check received sync ack
if insync == '\x14' and ok == '\x10':
  print "insync"

print "device signature %s-%s-%s" % (ord(signature[0]), ord(signature[1]), ord(signature[2]))

# start with page address 0
address = 0

# open the hex file
program = open("main.hex", "rb")

while True:
  # calculate page address
  laddress = chr(address % 256)
  haddress = chr(address / 256)
  address += 64

  # load page address
  print "loading page address"
  sock.send('\x55') # STK_LOAD_ADDRESS
  sock.send(laddress)
  sock.send(haddress)
  sock.send('\x20') # SYNC_CRC_EOP
  #time.sleep(0.01)

  # receive sync ack
  print "receiving sync ack"
  insync = sock.recv(1) # STK_INSYNC
  ok = sock.recv(1) # STK_OK

  # check received sync ack
  if insync == '\x14' and ok == '\x10':
    print "insync"

  data = ""
  # 16 bytes in a line 16 * 8 = 128
  for i in range(8):
    # just take the data part
    data += program.readline()[9:][:-4]

  size = chr(len(data)/2)
  print "sending program page to write", ord(size)
  sock.send('\x64') # STK_PROGRAM_PAGE
  sock.send('\x00') # page size
  sock.send(size) # page size
  sock.send('\x46') # flash memory, 'F'
  # while data left
  while data:
    # assemble a byte and send it
    sock.send(chr(int(data[:2], 16)))
    # chop of sent data
    data = data[2:]
  sock.send('\x20') # SYNC_CRC_EOP
  #time.sleep(0.01)

  # receive sync ack
  print "receiving sync ack"
  insync = sock.recv(1) # STK_INSYNC
  ok = sock.recv(1) # STK_OK

  # check received sync ack
  if insync == '\x14' and ok == '\x10':
    print "insync"

  # when the whole program was uploaded
  if size != '\x80':
    break

# close the hex file
program.close()

# load page address
#print "loading page address"
#sock.send('\x55') # STK_LOAD_ADDRESS
#sock.send('\x00')
#sock.send('\x00')
#sock.send('\x20') # SYNC_CRC_EOP
#time.sleep(0.05)

# receive sync ack
#print "receiving sync ack"
#insync = sock.recv(1) # STK_INSYNC
#ok = sock.recv(1) # STK_OK

# check received sync ack
#if insync == '\x14' and ok == '\x10':
#  print "insync"

# send read program page
#print "sending program page to read"
#sock.send('\x74') # STK_READ_PAGE
#sock.send('\x00') # page size
#sock.send('\x80') # page size
#sock.send('\x46') # flash memory, 'F'
#sock.send('\x20') # SYNC_CRC_EOP
#time.sleep(0.05)

#print len(sock.recv(128))

# leave programming mode
print "leaving programming mode"
sock.send('\x51') # STK_LEAVE_PROGMODE
sock.send('\x20') # SYNC_CRC_EOP
time.sleep(0.05)

# receive sync ack
print "receiving sync ack"
insync = sock.recv(1) # STK_INSYNC
ok = sock.recv(1) # STK_OK

# check received sync ack
if insync == '\x14' and ok == '\x10':
  print "insync"

# close the bluetooth connection
sock.close()