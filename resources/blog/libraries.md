http://stuartsierra.com/2013/07/28/the-amateur-problem

For me, one of Clojure's strengths in the tendency for people to create libraries that do one thing (and one thing only). In contrast, Java has too many frameworks - when you choose one you are locked out of the benefits of another. I think that's the important difference between libraries and frameworks - libraries compose well with other libraries (at least in Clojure), frameworks don't.

Earlier this year I worked at Likely in London. They built some excellent services on a set of very small libraries, this policy enabled a degree of re-use I have never witnessed in Java shops. Once functionality is in a library, multiple uses will drive the design improvements. If equivalent functionality is locked up in an application, it's less likely to be improved as rapidly. Of course, for this to work you need the version control infrastructure (such as github) to facilitate distributed contributions.
