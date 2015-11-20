#
# This is the source code for the user interface.
#
# The HTML, stylesheet and necessary javascript files are automatically added
#  by the fluid compiler.
#
# You can install the fluid compiler using:
#     npm install fluid-lang -g
#
# The `fluid` compiler command looks something like this:
#     fluid --compile app.coffee \
#       --include-js lib/thrift/lib/js/src/thrift.js \
#       --include-js gen-js/web_types.js \
#       --include-js gen-js/Web.js
#
# See this app's package.json for more tasks.
#


# Create a ref to the app server via the bundled Apache Thrift 
#   XHR/JSON RPC client.
# Also, export it to the window so that we can run RPC calls
#   from the in-browser Fluid REPL.
window.server = server = new App.WebClient new Thrift.TJSONProtocol new Thrift.TXHRTransport '/rpc'

# Set the application title
app.title 'Ask Craig'

# Hide the footer
hide app.footer

# This is a function that creates the UI for a list of jobs.
createJobListing = (jobs) ->

  # A job listing is a block of jobs, each of which is a card.
  block jobs.map (job) ->
    card
      # The card displays the title and category.
      items: [
        body2 job.title
        caption "Category: #{job.category}"
      ]

      # The card has a menu with an Edit command .
      menu: menu [
        command 'Edit', -> alert 'not implemented'
      ]

      # Allow the card to expand to available width, and collapse to
      #   minimum height.
      style: 
        width: 'auto'
        minHeight: 'auto'

# This is a function that fetches latest jobs from the server
refreshJobListings = ->
  server.listJobs 0, 25, (jobs) ->
    # Fill the job listing view with jobs, if available.
    if jobs then set jobListingView, createJobListing jobs

# This is a function that creates a view to add/edit a job posting.
createJobView = ->

  # The job title field is a text area.
  jobTitleField = textarea title: 'Job Description'

  # The job category field is a text field.
  jobCategoryField = textfield 'NA', title: 'Category'

  # Tells the app server to save the new job posting.
  createJob = ->
    # A new job is composed of the values from our two fields.
    job = new App.Job
      title: get jobTitleField
      category: get jobCategoryField

    # Create the job...
    server.createJob job, ->
      # ... clear the title field
      set jobTitleField, '' 

      # ... and go back to our list of jobs.
      showJobListingView()

  # The form itself is a card containing our two fields.
  self = card
    title: 'Add a new job posting'
    items: [
      block jobTitleField
      block jobCategoryField
    ]
    buttons: [
      # The post button creates a job and saves the economy ftw.
      button 'Post', color: 'accent', createJob

      # The dismiss button simply goes back to our list of jobs.
      button 'Dismiss', showJobListingView
    ]
    # Expand the form's width to fit the screen.
    style: 
      width: 'auto'

  # This function prefills the job category, given a title.
  prefillJobCategory = (jobTitle) ->
    # Ask the server for the predicted category...
    server.predictJobCategory jobTitle, (category) ->
      # ... then update the job category field.
      set jobCategoryField, category

  # Whenever the job title changes, prefill the job category,
  #   (but do this once in 500ms to go easy on the server).
  bind jobTitleField, _.throttle prefillJobCategory, 500

  self

# Shows the job listings.
showJobListingView = ->
  hide jobView
  show jobListingView
  show buttonContainer
  refreshJobListings()

# Shows the job view.
showJobView = ->
  hide jobListingView
  hide buttonContainer
  show jobView

# Create a job listing view (an empty block).
jobListingView = block()

# Create a job view.
jobView = createJobView()

# Create a FAB (a floating action button).
addJobButton = button 'add', type: 'floating', color: 'primary'

# Put the button in a block positioned to the bottom right,
#    on top of everything else.
buttonContainer = block addJobButton, 
  style: 
    position: 'absolute'
    right: '10px'
    bottom: '10px'
    zIndex: 100

# Show the job view when the button is pressed.
bind addJobButton, showJobView

# Set the contents of the active page .
set activePage, [ jobListingView, buttonContainer, jobView ]

# Show the job listing as our initial view.
showJobListingView()

