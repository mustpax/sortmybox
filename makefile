alljs = public/js/all.js
jsfiles = public/js/json2.min.js public/js/jquery-1.7.2.min.js public/js/bootstrap.min.js public/js/underscore-min.js public/js/jquery-ui-1.8.20.custom.min.js public/js/sortbox.js
play = submodules/play/framework/play-local.jar
play-gae = submodules/play-gae/lib/play-gae.jar

all: deps js conf/secret.conf ${play} 
	build/prep-webxml.py

test: all
	play test

run: all
	play run -ea

js: $(alljs)

$(alljs): $(jsfiles)
	cat $(jsfiles) > $@

static: all
	build/sync-bucket.sh

deps: ${play-gae} ${play} .lastdepsrun

.lastdepsrun: conf/dependencies.yml
	play deps
	play ec
	date > .lastdepsrun

conf/secret.conf:
	cp conf/secret.conf.template conf/secret.conf

submodules:
	git submodule update --init

${play}: submodules
	ant -f submodules/play/framework/build.xml -Dversion=local

${play-gae}: ${play} submodules
	ant -f submodules/play-gae/build.xml -Dplay.path="`pwd`/submodules/play"

stage: all static
	build/checkbranch.sh staging
	-play gae:deploy
	git push origin staging

deploy: all static
	build/checkbranch.sh prod
	play gae:deploy
	git push origin prod

dev: all static
	play gae:deploy

lint:
	jshint public/

clean:
	-play clean
	-rm $(alljs)
	-rm ${play}
	-rm ${play-gae}
	-rm .lastdepsrun
	-rm lib/*
	-rm -rf modules/
	-rm -rf tmp/

auto-test: conf/secret.conf ${play}
	play auto-test --deps

superclean:
	# RUN THIS AT YOUR OWN RISK, THIS WILL DELETE EVERY UNTRACKED FILE 
	git clean -f

.PHONY : all run js static deps stage deploy dev clean superclean lint auto-test submodules
