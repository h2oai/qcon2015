Thrift = require 'thrift'
ML = require './gen-nodejs/AskCraig'

{ ML_HOST } = process.env

unless ML_HOST then throw new Error 'ML_HOST not specified.'

[ ip, port ] = ML_HOST.split ':'

console.log "Connecting to ML server #{ML_HOST} ..."
connection = Thrift.createConnection ip, port,
  transport : Thrift.TBufferedTransport()
  protocol : Thrift.TBinaryProtocol()

connection.on 'error', (error) ->
  console.error 'Connection failure:'
  console.error error

connection.on 'connect', ->
  console.log 'Connected.'

  console.log 'Creating ML client ...'
  ml = Thrift.createClient ML, connection

  console.log 'Retraining ...'
  ml.buildModel 'data/craigslistJobTitles.csv', (error, result) ->
    if error
      console.error 'Retraining failed:'
      console.error error
    else
      console.log 'Retraining completed.'

    console.log 'Disconnecting ...'
    connection.end()
    console.log 'Disconnected'
