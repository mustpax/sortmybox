alljs = public/js/all.js
jsfiles = public/js/json2.min.js public/js/jquery-1.7.2.min.js public/js/bootstrap.min.js public/js/underscore-min.js public/js/jquery-ui-1.8.20.custom.min.js public/js/sortbox.js
play = submodules/play/framework/play-local.jar
play-gae = submodules/play-gae/lib/play-gae.jar

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
	build/checksubmodules.sh
	git submodule update --init
	ant -f submodules/play/framework/build.xml -Dversion=local

${play-gae}: ${play}
	build/checksubmodules.sh
	git submodule update --init
	play deps submodules/play-gae --sync
	ant -f submodules/play-gae/build.xml -Dplay.path="`pwd`/submodules/play"

.PHONY: package
package: all
	play gae:package

.PHONY: dispatch
dispatch: all
	build/checkbranch.sh prod
	play gae:update_dispatch

.PHONY: default_version
default_version: all
	build/checkbranch.sh prod
	@build/confirm.py "Set default version to **`cat VERSION`** for all modules?"
	appcfg.sh set_default_version war/background
	appcfg.sh set_default_version war/default

.PHONY: stage
stage: all
	build/checkbranch.sh staging
	-play gae:deploy
	git push origin staging

.PHONY: deploy
deploy: all
	build/checkbranch.sh prod
	play gae:deploy
	git push origin prod

.PHONY: dev
dev: all
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
