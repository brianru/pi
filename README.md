**NOTE everything is changing all the time. there are no quality guarantees.**

# [Project Pi (real name tbd)](https://project-pi.herokuapp.com)

This is a hyper local, ephemeral information platform.

It's like a local reddit.

You cannot choose where your posts go, you can only say something where you are. It is not primarily topic-driven.

The 'information radius' is a function of the usage and population density of your location.

Users will be able to teleport to different locations to read, but not contribute to the conversation.

Users will also be able to travel through time, because why not?

## Usage

### Dependencies

- [Leiningen.](http://leiningen.org/) That's it.

### How To
**NOTE this is changing -- adding Datomic ->> gotta have a transactor
running**

#### Local
```bash
$ lein cljsbuild auto dev
$ lein run dev
```
#### Server
```bash
$ lein uberjar
```

### Documentation

https://rawgit.com/brianru/pi/master/docs/uberdoc.html
TODO logically sort namespaces

To generate docs, run:
```bash
lein marg src src-cljs
```

## License

Copyright Brian James Rubinton © 2014 FIXME
