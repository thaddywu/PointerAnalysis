import os
import shutil

classes = [  # Basic Tests
    "Hello",
    "Hello2",
    "FieldSensitivity2",
    "FlowSensitivity1",
    "FlowSensitivity2",
    "Recursion",
    "ContextSensitivity1",
    "Function",
    "Function2",
    "StaticFieldRef",
    "ImplicitAllocId",
    "Inheritance",
    "ForLoop",
    "ForLoopComplex",
    "If",
    "WeakUpdate",
    "FieldInField",
    # Advanced Tests
    "FieldSensitivity",
    "PointerInHeap",
    "RecursionComplex",
    "FinalTest"
]

proj_root = os.path.dirname(os.path.abspath(__file__))
os.chdir(proj_root)


def generate_class_file():
    if os.path.exists('sootOutput'):
        shutil.rmtree('sootOutput')
    # os.mkdir('sootOutput')

    shutil.copytree('sootInput', 'sootOutput')

    os.chdir(os.path.join(proj_root, 'sootOutput'))
    for root, dirs, files in os.walk('.'):
        for file in files:
            if not file.endswith('.java'):
                continue
            src_file = os.path.join(root, file)
            print('javac ' + src_file)
            os.system("javac " + src_file)


def build():
    os.chdir(proj_root)
    os.system('mvn clean package')
    shutil.copyfile('target\pta-1.0-SNAPSHOT-jar-with-dependencies.jar',
                    'analyzer.jar')


def run_analysis():

    if os.path.exists('myResults'):
        shutil.rmtree('myResults')
    os.mkdir('myResults')
    for class_file in classes:
        os.system(f'java -jar analyzer.jar sootOutput test.{class_file}')
        shutil.copyfile('result.txt',
                        os.path.join('myResults', class_file + '.txt'))


def compare():
    def file2dict(file):
        d = {}
        with open(file, 'r', encoding='utf-8') as f:
            for line in f.readlines():
                if line == '\n':
                    continue
                test_id, alloc_id_str = line.split(':')
                d[int(test_id)] = list(map(int, alloc_id_str.strip().split()))
        return d

    compare_file = open('compare_result.txt', 'w', encoding='utf-8')
    for class_file in classes:
        my_result_file = os.path.join('myResults', class_file + '.txt')
        correct_result_file = os.path.join('correctResults',
                                           class_file + '.txt')

        print(f'----------compare test.{class_file}')
        compare_file.write(f'----------compare test.{class_file}\n')
        my_res = file2dict(my_result_file)
        ground_truth = file2dict(correct_result_file)
        unsound = False
        for key in ground_truth:
            if key not in my_res:
                unsound = True
                continue
            for id in ground_truth[key]:
                if id not in my_res[key]:
                    unsound = True

        if unsound:
            print("Unsound")
            compare_file.write("Unsound\n")
        else:
            ground_truth_cnt = len(sum(ground_truth.values(), []))
            my_res_cnt = len(sum(my_res.values(), []))
            if my_res_cnt == 0:
                print("Precision=1")
                compare_file.write("Precision=1\n")
            else:
                print("Precision={:.4f}".format(ground_truth_cnt / my_res_cnt))
                compare_file.write("Precision={:.4f}\n".format(ground_truth_cnt / my_res_cnt))
        print('----------End\n')
        compare_file.write('----------End\n\n')


# generate_class_file()
build()
run_analysis()
compare()