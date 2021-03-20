# Reference: https://www.pyimagesearch.com/2018/09/24/opencv-face-recognition/
# Files imported into python directory
# - le.pickle
# - recognizer.pickle
# - face_embeddings.pickle

import os
import numpy as np
import pickle
import imutils
import cv2
import base64
from sklearn import preprocessing, svm
import face_recognition


def detect_faces(image_data):

    # Decode Image Data and Convert To Numpy
    decode_data = base64.b64decode(image_data)
    np_data = np.fromstring(decode_data, np.uint8)
    image_4_channels = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
    image = image_4_channels[...,:3]

    # Load Detector
    prototxt_filename = os.path.join(os.path.dirname(__file__), "deploy.prototxt")
    weight_filename = os.path.join(os.path.dirname(__file__), "res10_300x300_ssd_iter_140000.caffemodel")
    detector = cv2.dnn.readNetFromCaffe(prototxt_filename, weight_filename)

    resized_image = imutils.resize(image, width=600)

    # Image BLOB (Binary Large Object) - prepare image for classification by the model
    # image, scaling, spatial size, mean subtraction
    blob = cv2.dnn.blobFromImage(cv2.resize(resized_image, (300, 300)), 1.0, (300, 300), (104.0, 177.0, 123.0), swapRB=False, crop=False)

    # Face Detection
    detector.setInput(blob)
    detections = detector.forward()

    return detections, resized_image


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
                    roiBlob = cv2.dnn.blobFromImage(roi, 1.0 / 255, (96, 96), (0, 0, 0), swapRB=True, crop=False)
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


def detect_face_fr(image_data):
    # Decode Image Data and Convert To Numpy
    decode_data = base64.b64decode(image_data)
    np_data = np.fromstring(decode_data, np.uint8)
    image_4_channels = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
    img = image_4_channels[...,:3]

    labels, encodings = retrieve_encodings_labels()

    # Using Face Recognition
    imgSmall = cv2.resize(img, (0, 0), None, 0.25, 0.25)
    imgSmall = cv2.cvtColor(imgSmall, cv2.COLOR_BGR2RGB)

    facesCurFrame = face_recognition.face_locations(imgSmall)
    encodesCurFrame = face_recognition.face_encodings(imgSmall, facesCurFrame)

    # Loop Faces for Current Frame
    names = []
    for encodeFace, faceLoc in zip(encodesCurFrame, facesCurFrame):
        matches = face_recognition.compare_faces(encodings, encodeFace)
        faceDis = face_recognition.face_distance(encodings, encodeFace)
        matchIndex = np.argmin(faceDis)

        if matches[matchIndex]:
            name = labels[matchIndex].upper()
            names.append(name)

    return names


def retrieve_encodings_labels():

    lblFileName = os.path.join(os.path.dirname(__file__), "labels.pickle")
    lblFile = open(lblFileName, "rb")
    labels = pickle.load(lblFile)
    lblFile.close()

    encFileName = os.path.join(os.path.dirname(__file__), "encodings.pickle")
    encFile = open(encFileName, "rb")
    encodings = pickle.load(encFile)
    encFile.close()

    return labels, encodings