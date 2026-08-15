package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.adapter.DaveyPostProcessor.PredictionMaps;
import Marshmello.MarshmelloWas.domain.checkin.adapter.OnnxImagePreprocessor.PreparedImage;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import java.util.Map;

final class DaveyOnnxModelRunner {

    private final OrtEnvironment environment;
    private final ModelSession anatomyLocator;
    private final ModelSession stretchUnet;
    private final int imageSize;

    DaveyOnnxModelRunner(
            OrtEnvironment environment,
            OrtSession anatomyLocator,
            OrtSession stretchUnet
    ) throws OrtException {
        this.environment = environment;
        this.anatomyLocator = validateModel(anatomyLocator, 2, "anatomy locator");
        this.stretchUnet = validateModel(stretchUnet, 1, "stretch U-Net");
        this.imageSize = inputImageSize(this.anatomyLocator.session());
        if (imageSize != inputImageSize(this.stretchUnet.session())) {
            throw new IllegalStateException("ONNX models must use the same input image size");
        }
    }

    int imageSize() {
        return imageSize;
    }

    PredictionMaps predict(PreparedImage prepared) throws OrtException {
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, prepared.tensor())) {
            float[][][][] anatomy = run(anatomyLocator, tensor);
            float[][][][] stretch = run(stretchUnet, tensor);
            return new PredictionMaps(
                    prepared.restoreProbability(anatomy[0][0]),
                    prepared.restoreProbability(anatomy[0][1]),
                    prepared.restoreProbability(stretch[0][0]));
        }
    }

    private ModelSession validateModel(
            OrtSession session,
            int expectedOutputChannels,
            String role
    ) throws OrtException {
        if (session.getNumInputs() != 1 || session.getNumOutputs() != 1) {
            throw new IllegalStateException(role + " must have exactly one input and one output");
        }
        NodeInfo input = session.getInputInfo().values().iterator().next();
        NodeInfo output = session.getOutputInfo().values().iterator().next();
        TensorInfo inputTensor = tensorInfo(input, role + " input");
        TensorInfo outputTensor = tensorInfo(output, role + " output");
        long[] inputShape = inputTensor.getShape();
        long[] outputShape = outputTensor.getShape();
        if (inputTensor.type != OnnxJavaType.FLOAT
                || inputShape.length != 4
                || inputShape[1] != 3
                || inputShape[2] <= 0
                || inputShape[2] != inputShape[3]) {
            throw new IllegalStateException(role + " input must be float NCHW with square RGB images");
        }
        if (outputTensor.type != OnnxJavaType.FLOAT
                || outputShape.length != 4
                || outputShape[1] != expectedOutputChannels
                || outputShape[2] != inputShape[2]
                || outputShape[3] != inputShape[3]) {
            throw new IllegalStateException(role + " output tensor contract does not match");
        }
        return new ModelSession(session, input.getName(), output.getName());
    }

    private TensorInfo tensorInfo(NodeInfo node, String role) {
        if (node.getInfo() instanceof TensorInfo tensorInfo) {
            return tensorInfo;
        }
        throw new IllegalStateException(role + " must be a tensor");
    }

    private int inputImageSize(OrtSession session) throws OrtException {
        NodeInfo input = session.getInputInfo().values().iterator().next();
        return Math.toIntExact(tensorInfo(input, "model input").getShape()[2]);
    }

    private float[][][][] run(ModelSession model, OnnxTensor input) throws OrtException {
        try (OrtSession.Result result = model.session().run(Map.of(model.inputName(), input))) {
            Object value = result.get(model.outputName())
                    .orElseThrow(() -> new IllegalStateException("ONNX output is missing"))
                    .getValue();
            if (value instanceof float[][][][] output) {
                return output;
            }
            throw new IllegalStateException("ONNX output must be a four-dimensional float tensor");
        }
    }

    private record ModelSession(OrtSession session, String inputName, String outputName) {}
}
