# juxtweb

This is the repository containing the content and code that drives the [JUXT website](https://juxt.pro).

## Static content

Static content is stored in another repository, and must be located under ```../website-static```. Clone this first, before you run.

    git clone https://github.com/juxt/website-static.git

## Develop

Development is via [Jig](https://github.com/juxt/jig).

    $ lein repl
    user> (go)

## Resources

Resources, including article content are under ```/resources```.

### Home page

The home page source is ```index.html.mustache```, with various links from that to Markdown snippets, also in the ```/resources``` directory.

### Articles

Articles are indexed in ```/resources/articles.edn```, which contains publication date (you can use this to schedule an article's publication), and other article metadata.

Article content is stored under ```/resources/articles```. The format for articles is HTML5, not Markdown, due to limitations in the Markdown syntax for doing rich dynamic content.

Articles can be embedded in HTML boilerplate for static rendering and development.

Articles are rendered via Stencil, so can contain mustache elements.

To enable the extraction of abstracts (article summaries) and table-of-contents generation, each article should follow this structure :-

    <article>
      <header>
        <hgroup>
          <h1>
            {{title}}
          </h1>
        </hgroup>
        <aside class="abstract">
          <p>
            ... abstract goes here ...
          </p>
          <p>
            (it can contain multiple paragraphs if necessary)
          </p>
        </aside>
      </header>
      <section>
        <h1>
          Introduction (but doesn't have to be called that)
        </h1>
        <p>
          blah...
        </p>
      <section>
      <section>
        <h1>
          ... another title ...
        </h1>
        <p>
          blah...
        </p>
      <section>
    </article>

For bigger articles, _chunking_ can be enabled via ```/resources/articles.edn```. Chunked articles get a table-of-contents generated and can be rendered a section at a time.

## Deployment

Once you are happy with changes, make sure you commit and push (the deploy script will check this for you).

Ensure you have SSH access to the ```juxtweb``` user on ```juxt.pro```.

Run the deploy script :-

    bin/deploy

## License

Copyright © 2013, JUXT

Distributed under the Eclipse Public License, the same as Clojure.

## Credits

Built with [Jig](https://github.com/juxt/jig), a harness for developing
and deploying modular Clojure applications.
