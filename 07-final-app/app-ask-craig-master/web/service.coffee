_ = require 'underscore'
ML = require './gen-nodejs/askcraig_types'
App = require './gen-nodejs/web_types.js'

module.exports = (db, workflow) ->

  ping = (go) -> go null, 'ACK'

  echo = (message, go) -> go null, message

  predictJobCategory = (jobTitle, go) ->
    console.log 'Predicting job category ...'
    workflow.predict jobTitle, (error, prediction) ->
      if error
        console.error error
        go error
      else
        console.log jobTitle
        console.dir prediction
        go null, prediction.label

  createJob = (job, go) ->
    console.log 'Creating job ...'

    jobs = db.collection 'jobs'
    jobs.insert { jobtitle: job.title, category: job.category }, (error, result) ->
      if error
        console.error error
        go error
      else
        console.log "Created #{result.insertedCount}."
        go null

  listJobs = (skip=0, limit=20, go) ->
    console.log 'Listing jobs ...'

    jobs = db.collection 'jobs'
    jobs.find {}, { skip, limit }, (error, jobs) ->
      list = []
      jobs.each (error, job) ->
        if error
          console.error error
          go error
        else
          if job
            list.push new App.Job title: job.jobtitle, category: job.category
          else
            console.log "Listed #{list.length}"
            go null, list

  { ping, echo, createJob, listJobs, predictJobCategory }

