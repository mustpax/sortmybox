# Makefile for the anza project
all: deps

run: all
	play run

deps: .lastdepsrun

.lastdepsrun: conf/dependencies.yml
	play deps --sync
	play ec
	date > .lastdepsrun

deploy:
	build/checkbranch.sh prod
	-play gae:deploy
	git push origin prod

clean:
	play clean

superclean:
	# RUN THIS AT YOUR OWN RISK, THIS WILL DELETE EVERY UNTRACKED FILE 
	git clean -dxf

