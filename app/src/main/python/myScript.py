import os
import numpy as np
import pickle
import cv2
import base64
from sklearn import preprocessing, svm

def box_axis(value):
    if len(value) == 4:
        x1 = value[0]
        y1 = value[1]
        x2 = value[2]
        y2 = value[3]
        return f"{x1} {y1} {x2} {y2}"
    return "no values"

def create_recognizer_and_labels():

    # Load Face Embeddings
    face_embeddings_filename = os.path.join(os.path.dirname(__file__), "face_embeddings.pickle")
    face_embeddings_pickle_out = open(face_embeddings_filename, 'rb')
    face_embeddings = pickle.load(face_embeddings_pickle_out)

    le = preprocessing.LabelEncoder()
    labels = le.fit_transform(face_embeddings['names'])

    # Train Recognizer
    recognizer = svm.SVC(C=1.0, kernel="linear", probability=True)
    recognizer.fit(face_embeddings["embeddings"], labels)

    # To Pickle
    le_filename = os.path.join(os.path.dirname(__file__), "le.pickle")
    le_pickle_out = open(le_filename, 'wb')
    pickle.dump(le, le_pickle_out)
    le_pickle_out.close()

    recognizer_filename = os.path.join(os.path.dirname(__file__), "recognizer.pickle")
    recognizer_pickle_in = open(recognizer_filename, 'wb')
    pickle.dump(recognizer, recognizer_pickle_in)
    recognizer_pickle_in.close()

    return "Success"


def detect_faces(image_data):

    # Decode Image Data and Convert To Numpy
    decode_data = base64.b64decode(image_data)
    np_data = np.fromstring(decode_data, np.uint8)
    image_4_channels = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
    image = image_4_channels[...,:3]

    # Load Neural Network
    prototxt_filename = os.path.join(os.path.dirname(__file__), "deploy.prototxt")
    weight_filename = os.path.join(os.path.dirname(__file__), "res10_300x300_ssd_iter_140000.caffemodel")
    net = cv2.dnn.readNetFromCaffe(prototxt_filename, weight_filename)

    # Image BLOB (Binary Large Object) - prepare image for classification by the model
    # image, scaling, spatial size, mean subtraction
    resized_image = cv2.resize(image, (300, 300))
    blob = cv2.dnn.blobFromImage(resized_image, 1.0, (300, 300), (104.0, 177.0, 123.0))

    # Face Detection
    net.setInput(blob)
    detections = net.forward()

    return detections, image


def do_detect_faces(image_data):
    detections, image = detect_faces(image_data)
    return detections.shape


def recognize_face(image_data):

    # Embedder to Extract the Features of the Face - Open Face Model
    embedder_filename = os.path.join(os.path.dirname(__file__), "openface.t7")
    embedder = cv2.dnn.readNetFromTorch(embedder_filename)

    # Recognizer to Predict the Feature of the Face
    recognizer_filename = os.path.join(os.path.dirname(__file__), "recognizer.pickle")
    recognizer_pickle_in = open(recognizer_filename, 'rb')
    recognizer = pickle.load(recognizer_pickle_in)

    # Names of the Faces
    label_encoder_filename = os.path.join(os.path.dirname(__file__), "le.pickle")
    label_encoder_pickle_in = open(label_encoder_filename, 'rb')
    label_encoder = pickle.load(label_encoder_pickle_in)

    detections, image = detect_faces(image_data)

    # Faces Recognized
    faces_recognized = {}

    # Faces Detected - Recognize Each Face (0, 1, 200, 7)
    for i in range(0, detections.shape[2]):

        # Range: 0 to 1
        confidence = detections[0, 0, i, 2]

        # Confidence Threshold
        if confidence > 0.5:

            # Bounding Box
            h, w = image.shape[:2]  # exclude no. of channels
            box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
            x1, y1, x2, y2 = box.astype("int")  # numpy array values to int

            # ROI: Region of Interest - Crop Face
            roi = image[y1:y2, x1:x2]
            roiH, roiW = roi.shape[:2]

            # Features of ROI
            if roiH >= 20 or roiW >= 20:
                try:
                    roiBlob = cv2.dnn.blobFromImage(roi, 1.0 / 255, (96, 96), swapRB=True, crop=False)
                except:
                    break
                embedder.setInput(roiBlob)
                vector = embedder.forward()

                # Classification
                predictions = recognizer.predict_proba(vector)[0]
                max_pred = np.argmax(predictions)
                probability = predictions[max_pred]
                face_recognized_name = label_encoder.classes_[max_pred]

                faces_recognized[i] = [face_recognized_name, probability, box]

    return faces_recognized





def type_of(value):
    return type(value)


def image_shape(image_data):
    decode_data = base64.b64decode(image_data)
    np_data = np.fromstring(decode_data, np.uint8)
    image = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
    return image.shape

def create_path():

    files = []
    for dir_path, sub_dir_names, file_names in os.walk(os.path.dirname(__file__)):
        for file_name in file_names:
            files.append(file_name)

    # embedder_filename = os.path.join(os.path.dirname(__file__), "res10_300x300_ssd_iter_140000.caffemodel")
    return os.path.dirname(__file__)