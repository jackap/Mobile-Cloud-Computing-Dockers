FROM alpine:3.4
RUN apk add --no-cache py-pip
RUN apk add --no-cache build-base python-dev py-pip jpeg-dev zlib-dev gcc
RUN apk add tesseract-ocr --update-cache --repository http://nl.alpinelinux.org/alpine/edge/community
ENV LIBRARY_PATH=/lib:/usr/lib
RUN pip install --upgrade pip
RUN pip install pytesseract Flask Pillow pymongo Flask-Login sorl-thumbnail Flask-SSLify
COPY ./ /usr/src/server
COPY ./language/eng.traineddata /usr/share/tessdata/
WORKDIR /usr/src/server
