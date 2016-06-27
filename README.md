# lein-voom-testing

This repository serves two purposes.

Firstly, it hosts generative test suite for testing [lein-voom](https://github.com/LonoCloud/lein-voom/).

Secondly, the generative test suite generally shows a model for
applying generative testing to systems with very complex interactions
that are difficult to test.

# Notes

The following notes are here to organize my thoughts. They'll probably not be intelligible to anyone else.

## Approach
1. generate stream of 'potential,' raw events
1. reify raw events against accumulating state to be either specific events or NOOPs
1. apply specific events against the actual system

- the initial state may be blank or may be seeded to a particular
  state from a sequence of pre-selected events. This helps the test
  creators to guide the tests to certain test scenarios.
- generators may emit multiple events to ensure certain specific, meaningful events occur
- events may be system actions or in-stream test events

## Test-run data
Hm. This wants to be datascript.

- test-run
  - state
     * projects - derived?
       - name
       - repo-source
       - repo-path
     * repo
        * source
           * ref
             - refname
             - tip-commit
             - ancestry dag
        * agent
           * ref
             - refname (master/branches/WC?)
             - tip-commit
             - ancestry dag
     - commits
        * parents
        - commit/author time
        * path
           - project name
           - project version
           - project deps
           - source file
      - event uuid -> commit
      - commit -> event uuid ?
  - actions []

## Minimum-viable test seed
- new origin-a, containing project-a
- new agent-a, points to origin-a
- new origin-b, containing project-b
- new agent-b, points to origin-b
- new dependency: project-b depends on project-a

- agents commit to main.clj, build-deps and freshen

## Stateful actions (currently for single committers)
- git init
- git clone
- write file
- git add
- git commit
- git push
- lein voom build-deps
- lein voom freshen
- lein run

## Later stateful actions
- git pull / merge
- lein deploy file:///

## Agents have their own:
- .../.voom-repos
- .../.m2
- WC directories

## Consider commit times:
- real wall clock
- instantaneous - monotonic
- random - monotonic
- second - monotonic
- day - monotonic
- year - monotonic
- sometimes retrograde
- totally random
-- also same vs. different GIT_AUTHOR_DATE & GIT_COMMITTER_DATE


## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.