alljs = public/js/all.js
jsfiles = public/js/json2.js public/js/jquery-1.6.2.min.js public/js/bootstrap.min.js public/js/underscore-min.js

all: deps js
	build/prep-webxml.py

run: all
	play run

js: $(alljs)

$(alljs): $(jsfiles)
	cat $(jsfiles) > $@

deps: .lastdepsrun

.lastdepsrun: conf/dependencies.yml
	play deps --sync
	play ec
	date > .lastdepsrun

stage: all
	build/checkbranch.sh staging
	-play gae:deploy
	git push origin staging

deploy: all
	build/checkbranch.sh prod
	-play gae:deploy
	git push origin prod

clean:
	play clean


superclean:
	# RUN THIS AT YOUR OWN RISK, THIS WILL DELETE EVERY UNTRACKED FILE 
	git clean -dxf

