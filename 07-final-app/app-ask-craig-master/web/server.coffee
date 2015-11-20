_ = require 'underscore'
Thrift = require 'thrift'
workflowServer = require './gen-nodejs/AskCraig'
appServer = require './gen-nodejs/Web.js'
MongoClient = require('mongodb').MongoClient
Service = require './service.coffee'

{ ML_HOST, DB_HOST, APP_PORT } = process.env

unless ML_HOST then throw new Error 'ML_HOST not specified.'

unless DB_HOST then throw new Error 'DB_HOST not specified.'

unless APP_PORT then throw new Error 'APP_PORT not specified.'

_isConnectedToWorkflow = no
connectToWorkflow = (ip, port, go) ->
  connection = Thrift.createConnection ip, port,
    transport : Thrift.TBufferedTransport()
    protocol : Thrift.TBinaryProtocol()

  connection.on 'error', (error) ->
    if not _isConnectedToWorkflow and error?.code is 'ECONNREFUSED'
      process.stdout.write '.'
      #TODO fail after 10m
      setTimeout connectToWorkflow, 1000, ip, port, go
    return

  connection.on 'connect', ->
    _isConnectedToWorkflow = yes
    process.stdout.write ' connected.\n'
    go null, connection: connection, client: client

  client = Thrift.createClient workflowServer, connection

connectToDatabase = (go) ->
  databaseHost = "mongodb://#{DB_HOST}/app-ask-craig"
  process.stdout.write "Connecting to #{databaseHost} ..."
  MongoClient.connect databaseHost, (error, db) ->
    if error
      go error
    else
      process.stdout.write ' connected.\n'
      go null, db

startServer = (db, workflow) ->
  process.stdout.write 'Starting app server ...'
  server = Thrift.createWebServer
    files: '.'
    services:
      '/rpc':
        transport: Thrift.TBufferedTransport
        protocol: Thrift.TJSONProtocol
        processor: appServer
        handler: Service db, workflow.client

  server.listen port = parseInt APP_PORT, 10

  process.on 'SIGTERM', ->
    console.log 'Shutting down.'
    workflow.connection.end()
    db.close() 
    process.exit 0

  process.stdout.write " started on port #{port}.\n"

main = ->
  [ ip, port ] = ML_HOST.split ':'
  process.stdout.write "Connecting to workflow server #{ip}:#{port} ..."

  connectToWorkflow ip, port, (error, workflow) ->
    if error then throw error
    connectToDatabase (error, db) ->
      if error then throw error
      startServer db, workflow

main()
