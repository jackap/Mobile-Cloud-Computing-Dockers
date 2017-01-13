from flask import Flask, Response
from flask.json import jsonify
from bson.objectid import ObjectId
from bson.binary import Binary
from pymongo import MongoClient
import pymongo
from PIL import Image
from pytesseract import image_to_string
import io
import time
import ast
from flask import request, redirect, url_for, render_template, session
from flask_login import LoginManager, UserMixin, login_required, login_user, logout_user, current_user
import base64
import datetime
import uuid
import math

UPLOAD_FOLDER = '/usr/src/server'
ALLOWED_EXTENSIONS = set(['png', 'jpg', 'jpeg', 'gif'])

app = Flask(__name__)
app.config.update(dict(
  PREFERRED_URL_SCHEME = 'https'
))

# file upload config
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

# session config
app.config['SECRET_KEY'] = 'sup3rs3kr1t'

# user and password config
app.config['USERNAMES'] = ['user', 'admin']
app.config['PASSWORDS'] = {'user': 'password1', 'admin': 'password2'}

# flask-login
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = "login"

# database config
client = MongoClient('mongo-database')

db = client['image_database']
image_collection = db['images']
history_collection = db['history']


class User(UserMixin):

    def __init__(self, id):
        self.id = id
        self.name = "user" + str(id)
        self.password = self.name + "_secret"

    def __repr__(self):
        return "%d/%s/%s" % (self.id, self.name, self.password)


@app.route("/login", methods=['GET', 'POST'])
def login():
    if request.method == 'GET':
        return render_template('login.html')
    elif request.method == 'POST':
        # handle hardcoded user login
        username = request.form['username']
        password = request.form['password']
        if not username or not password:
            return jsonify({"status": 400, 'message': 'username and password are mandatory parameters'})
        else:
            if password == username + "_secret":
                try:
                    id = username.split('user')[1]
                except Exception:
                    return redirect(url_for('login'))

                user = User(id)
                login_user(user)
                return redirect(url_for('remote_ocr'))
            else:
                return redirect(url_for('login'))
        return jsonify(request.form)


# url to logout
@app.route("/logout")
@login_required
def logout():
    logout_user()
    return render_template('logout.html')


# callback to reload the user object
@login_manager.user_loader
def load_user(userid):
    return User(userid)


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS


@app.route('/remote_ocr', methods=['GET', 'POST'])
@login_required
def remote_ocr():
    """
    Remote OCR endpoint.
    Stores image to mongo and calls OCR endpoint to acquire the text.
    """
    if request.method == 'POST':
        # check if the post request has the file part
        if 'file[]' not in request.files:
            return redirect(request.url)
        files = request.files.getlist('file[]')

        # if user does not select file, browser also
        # submit a empty part without filename
        image_ids = list()
        for file in files:
            if file.filename == '':
                return redirect(request.url)
            if file and allowed_file(file.filename):
                bin_file = file.read()
                user_id = session['user_id']
                data = {'image': Binary(
                    bin_file), 'owner': user_id, 'original_filename': file.filename}

                # store the image to mongo
                image_id = image_collection.insert_one(data).inserted_id
                image_ids.append(str(image_id))

        return redirect(url_for('result', imgs=image_ids))

    return render_template('remote_ocr.html')


@app.route('/local_ocr')
@login_required
def local_ocr():
    return render_template('local_ocr.html')


@app.route('/image/<image>')
@login_required
def show_image(image):
    """
    Fetch the uploaded file from mongodb and display it
    """
    try:
        id_ = ObjectId(image)
    except:
        return jsonify(result={"status": 404})

    image = image_collection.find_one(ObjectId(id_))
    resp = Response(image['image'])
    resp.headers['mimetype'] = 'image/jpeg'
    resp.headers['Content-Type'] = 'image/jpeg'
    return resp


@app.route('/results/<imgs>')
@login_required
def result(imgs):
    """
    OCR the images, store results to mongo and return the results.
    """
    start = time.time()
    results = list()
    batch_id = str(uuid.uuid4())
    for image_id in ast.literal_eval(imgs):

        image = image_collection.find_one(ObjectId(image_id))
        original_filename = image["original_filename"]

        img = io.BytesIO(image['image'])

        size = 128, 128
        im = Image.open(img)
        im.thumbnail(size)
        output = io.BytesIO()
        im.save(output, format="jpeg")
        thumb_id = str(image_collection.insert_one(
            {"image": Binary(output.getvalue())}).inserted_id)

        txt = image_to_string(Image.open(img))
        time_now = datetime.datetime.utcnow()

        if txt == '':
            tmp = {"batch_id": batch_id, "text_found": False, "text": "No text found from image",
                   "image_id": image_id, "original_filename": original_filename, "owner": session['user_id'], 'date': time_now, 'thumb_id': thumb_id}
            results.append({"batch_id": batch_id, "text_found": False, "text": "No text found from image",
                            "image_id": image_id, "original_filename": original_filename, "owner": session['user_id'], 'date': time_now, 'thumb_id': thumb_id})
            history_collection.insert_one(tmp)
        else:
            tmp = {"batch_id": batch_id, "text_found": True, "text": txt,
                   "image_id": image_id, "original_filename": original_filename, "owner": session['user_id'], 'date': time_now, 'thumb_id': thumb_id}
            results.append({"batch_id": batch_id, "text_found": True, "text": txt,
                            "image_id": image_id, "original_filename": original_filename, "owner": session['user_id'], 'date': time_now, 'thumb_id': thumb_id})
            history_collection.insert_one(tmp)

    end = time.time()
    timedelta = (end - start)
    return jsonify(result={"status": 200, "texts": results, "execution_time": timedelta})


@app.route('/benchmark', methods=['GET', 'POST'])
@login_required
def benchmark():
    if request.method == 'GET':
        return render_template('benchmark.html')
    elif request.method == 'POST':
        # check if the post request has the file part
        if 'file[]' not in request.files:
            return jsonify({'status': 400})
        files = request.files.getlist('file[]')

        # if user does not select file, browser also
        # submit a empty part without filename
        results = {}
        results['total_time'] = 0
        results['images'] = {}
        times = list()
        for file in files:
            if file.filename == '':
                return jsonify({'status': 400})
            if file and allowed_file(file.filename):
                bin_file = file.read()
                img = io.BytesIO(bin_file)
                start = time.time()
                txt = image_to_string(Image.open(img))
                end = time.time()
                timedelta = (end - start)
                results['images'][file.filename] = timedelta
                times.append(timedelta)

        results['total_time'] = sum(times)
        results['average_time'] = results['total_time'] / (len(files))
        results['min_time'] = min(times)
        results['max_time'] = max(times)
        results['std_dev'] = math.sqrt(
            sum(map(lambda x: (x - results['average_time'])**2, times)) / len(files))
        return jsonify(results)

    else:
        return jsonify({'status': 400})


@app.route('/history/<user>')
@login_required
def history(user):
    """
    Retrieves the remote OCR history of a user.
    """
    usr = current_user
    if usr.id != user:
        return jsonify({"status": 403})

    hist = history_collection.find(
        {"owner": user}).sort("_id", pymongo.DESCENDING)
    results = dict()
    for item in hist:
        tmp = {"text_found": item["text_found"], "text": item["text"], "image_id": item["image_id"], "date": item["date"], "batch": item["batch_id"],
               "original_filename": item["original_filename"], "owner": item["owner"], "thumb_id": item["thumb_id"], "b64_text": base64.b64encode(item["text"].encode('utf-8'))}
        if item["batch_id"] in results:
            results[item["batch_id"]]['items'].append(tmp)
            results[item["batch_id"]]['full_text'] = results[
                item["batch_id"]]['full_text'] + " --- " + item["text"]
        else:
            results[item["batch_id"]] = {}
            results[item["batch_id"]]['items'] = list()
            results[item["batch_id"]]['items'].append(tmp),
            results[item["batch_id"]]['full_text'] = item["text"]

    for batch in results:
        results[batch]['full_text_b64'] = base64.b64encode(
            results[batch]['full_text'].encode('utf-8'))

    return render_template('history.html', results=results)


@app.route('/history_api/<user>')
@login_required
def history_api(user):
    """
    Retrieves the remote OCR history of a user.
    """
    usr = current_user
    if usr.id != user:
        return jsonify({"status": 403})

    hist = history_collection.find(
        {"owner": user}).sort("_id", pymongo.DESCENDING)
    results = dict()
    for item in hist:
        tmp = {"text_found": item["text_found"], "text": item["text"], "image_id": item["image_id"], "date": item["date"], "batch": item["batch_id"],
               "original_filename": item["original_filename"], "owner": item["owner"], "thumb_id": item["thumb_id"], "b64_text": base64.b64encode(item["text"].encode('utf-8'))}
        if item["batch_id"] in results:
            results[item["batch_id"]]['items'].append(tmp)
            results[item["batch_id"]]['full_text'] = results[
                item["batch_id"]]['full_text'] + " --- " + item["text"]
        else:
            results[item["batch_id"]] = {}
            results[item["batch_id"]]['items'] = list()
            results[item["batch_id"]]['items'].append(tmp),
            results[item["batch_id"]]['full_text'] = item["text"]

    for batch in results:
        results[batch]['full_text_b64'] = base64.b64encode(
            results[batch]['full_text'].encode('utf-8'))

    return jsonify({'status': 200, 'data': results})


@app.route('/')
def index_route():
    return jsonify({"status": 200})


app.run(host='0.0.0.0', port=80, threaded=True)
