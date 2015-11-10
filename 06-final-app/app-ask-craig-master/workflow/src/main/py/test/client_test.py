import sys, glob
sys.path.append('gen-py')
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from askcraig import AskCraig
from askcraig.ttypes import *
# Initialize
socket = TSocket.TSocket('localhost', 9090)
transport = TTransport.TBufferedTransport(socket)
protocol = TBinaryProtocol.TBinaryProtocol(transport)
client = AskCraig.Client(protocol)
transport.open()
# Get thrift files
client.getThrift()
# Get model labels
client.getLabels()
# Make a prediction
client.predict("Marketing manager for sparkling water!")
client.predict("Developer for sparkling water needed!")
