# Ask Craig

See these blog posts for more about this app:

http://h2o.ai/blog/2015/06/ask-craig-sparkling-water/

http://h2o.ai/blog/2015/07/ask-craig-sparkling-water-2/

## Steps to run

### Preconditions

1. brew install mongodb
2. brew install thrift

### Window 1

1. git clone git@github.com:h2oai/app-ask-craig.git
1. cd app-ask-craig
1. mkdir -p data/db
1. mongod --dbpath \`pwd\`/data/db

### Window 2

1. cd app-ask-craig
1. source env.sh
1. ./install.sh
1. ./start.sh
1. ./monitor.sh

Watch the monitor output to see the model train.

When you see this line:

```
Starting app server ... started on port 9091.
```

then go to this URL to interact with the app: <http://localhost:9091>

Push the "+" in the lower right hand corner to type in a new job title.
