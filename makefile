alljs = public/js/all.js
jsfiles = public/js/json2.min.js public/js/jquery-1.7.2.min.js public/js/bootstrap.min.js public/js/underscore-min.js public/js/jquery-ui-1.8.20.custom.min.js public/js/sortbox.js
play = submodules/play/framework/play-local.jar
play-gae = submodules/play-gae/lib/play-gae.jar
JAVA_HOME=$([[ -z "$TRAVIS" ]] && /usr/libexec/java_home -v 1.7 || echo $JAVA_HOME)

.PHONY: all
all: deps js conf/secret.conf
	build/prep-webxml.py

.PHONY: auto-test
auto-test: all
	play auto-test

.PHONY: test
test: all
	play test

.PHONY: run
run: all
	play run -ea

.PHONY: js
js: $(alljs)

$(alljs): $(jsfiles)
	cat $(jsfiles) > $@

.PHONY: static
static: all
	build/sync-bucket.sh

.PHONY: deps
deps: .lastdepsrun

.lastdepsrun: conf/dependencies.yml ${play-gae} ${play} 
	play deps
	play ec
	date > .lastdepsrun

conf/secret.conf:
	cp conf/secret.conf.template conf/secret.conf

${play}:
	git submodule update --init
	ant -f submodules/play/framework/build.xml -Dversion=local

${play-gae}: ${play}
	git submodule update --init
	play deps submodules/play-gae --sync
	ant -f submodules/play-gae/build.xml -Dplay.path="`pwd`/submodules/play"

.PHONY: stage
stage: all static
	build/checkbranch.sh staging
	-play gae:deploy
	git push origin staging

.PHONY: deploy
deploy: all static
	build/checkbranch.sh prod
	play gae:deploy
	git push origin prod

.PHONY: dev
dev: all static
	play gae:deploy

.PHONY: lint
lint:
	jshint public/

.PHONY: clean
clean:
	-play clean
	-rm $(alljs)
	-rm ${play}
	-rm ${play-gae}
	-rm .lastdepsrun
	-rm lib/*
	-rm -rf modules/
	-rm -rf tmp/
