package cloudsim.ext.datacenter;
import java.util.LinkedList;
import java.util.Queue;

class AVLTreeBalancer<T extends Comparable<T>> {
    Node<T> root;

    public AVLTreeBalancer() {
        root = null;
    }

    public T Maximum() {
        Node<T> local = root;
        if (local == null)
            return null;
        while (local.getRight() != null)
            local = local.getRight();
        return local.getVirtualMachinePreoccupancy();
    }

    public T Minimum() {
        Node<T> local = root;
        if (local == null)
            return null;
        while (local.getLeft() != null) {
            local = local.getLeft();
        }
        return local.getVirtualMachinePreoccupancy();
    }

    private int depth(Node<T> node) {
        if (node == null)
            return 0;
        return node.getDepth();
        // 1 + Math.max(depth(node.getLeft()), depth(node.getRight()));
    }

    public Node<T> insert(T data) {
        root = insert(root, data);
        switch (balanceNumber(root)) {
            case 1:
                root = rotateLeft(root);
                break;
            case -1:
                root = rotateRight(root);
                break;
            default:
                break;
        }
        return root;
    }

    public Node<T> insert(Node<T> node, T data) {
        if (node == null)
            return new Node<T>(data);
        if (node.getVirtualMachinePreoccupancy().compareTo(data) > 0) {
            node = new Node<T>( node.getVirtualMachinePreoccupancy(),
                                insert(node.getLeft(), data),
                                node.getRight()
            );
            // node.setLeft(insert(node.getLeft(), data));
        } else if (node.getVirtualMachinePreoccupancy().compareTo(data) < 0) {
            // node.setRight(insert(node.getRight(), data));
            node = new Node<T>( node.getVirtualMachinePreoccupancy(),
                                node.getLeft(), insert(
                                node.getRight(), data)
            );
        }
        // After insert the new node, check and balance the current node if
        // necessary.
        switch (balanceNumber(node)) {
            case 1:
                node = rotateLeft(node);
                break;
            case -1:
                node = rotateRight(node);
                break;
            default:
                return node;
        }
        return node;
    }

    private int balanceNumber(Node<T> node) {
        int L = depth(node.getLeft());
        int R = depth(node.getRight());
        if (L - R >= 2)
            return -1;
        else if (L - R <= -2)
            return 1;
        return 0;
    }

    private Node<T> rotateLeft(Node<T> node) {
        Node<T> q = node;
        Node<T> p = q.getRight();
        Node<T> c = q.getLeft();
        Node<T> a = p.getLeft();
        Node<T> b = p.getRight();
        q = new Node<T>(q.getVirtualMachinePreoccupancy(), c, a);
        p = new Node<T>(p.getVirtualMachinePreoccupancy(), q, b);
        return p;
    }

    public Node<T> getLeftMostDescendant(T data)
    {
        Node<T> local = search(data);
        if (local == null)
            return null;
        while (local.getLeft() != null) {
            local = local.getLeft();
        }
        return local;
    }
    private Node<T> rotateRight(Node<T> node) {
        Node<T> q = node;
        Node<T> p = q.getLeft();
        Node<T> c = q.getRight();
        Node<T> a = p.getLeft();
        Node<T> b = p.getRight();
        q = new Node<T>(q.getVirtualMachinePreoccupancy(), b, c);
        p = new Node<T>(p.getVirtualMachinePreoccupancy(), a, q);
        return p;
    }

    public Node<T> search(T data)
    {
        Node<T> local = root;
        while (local != null)
        {
            if (local.getVirtualMachinePreoccupancy().compareTo(data) == 0)
                return local;
            else if (local.getVirtualMachinePreoccupancy().compareTo(data) > 0)
                local = local.getLeft();
            else
                local = local.getRight();
        }
        return null;
    }

    public String toString() {
        return root.toString();
    }

    public void PrintTree() {
        root.level = 0;
        Queue<Node<T>> queue = new LinkedList<Node<T>>();
        queue.add(root);
        while (!queue.isEmpty())
        {
            Node<T> node = queue.poll();
            System.out.println(node);

            int level = node.level;
            Node<T> left = node.getLeft();
            Node<T> right = node.getRight();

            if (left != null) {
                left.level = level + 1;
                queue.add(left);
            }

            if (right != null) {
                right.level = level + 1;
                queue.add(right);
            }
        }
    }
}


// Our node here corresponds to a virtual machine in a datacenter
class Node<T extends Comparable<T>> implements Comparable<Node<T>> {

    private T virtualMachinePreoccupancy;
    private T vmID;
    private Node<T> left;
    private Node<T> right;
    public int level;
    private int depth;

    public Node(T data) {
        this(data, null, null);
    }

    public Node(T data, Node<T> left, Node<T> right) {
        super();
        this.virtualMachinePreoccupancy = data;
        this.left = left;
        this.right = right;
        if (left == null && right == null)
            setDepth(1);
        else if (left == null)
            setDepth(right.getDepth() + 1);
        else if (right == null)
            setDepth(left.getDepth() + 1);
        else
            setDepth(Math.max(left.getDepth(), right.getDepth()) + 1);
    }

    public T getVirtualMachinePreoccupancy() {
        return virtualMachinePreoccupancy;
    }

    public T getVmID()
    {
        return vmID;
    }


    public void setVirtualMachinePreoccupancy(T virtualMachinePreoccupancy) {
        this.virtualMachinePreoccupancy = virtualMachinePreoccupancy;
    }

    public Node<T> getLeft() {
        return left;
    }

    public void setLeft(Node<T> left) {
        this.left = left;
    }

    public Node<T> getRight() {
        return right;
    }

    public void setRight(Node<T> right) {
        this.right = right;
    }

    /**
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param depth
     *            the depth to set
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public int compareTo(Node<T> o) {
        return this.virtualMachinePreoccupancy.compareTo(o.virtualMachinePreoccupancy);
    }

    @Override
    public String toString() {
        return "Level " + level + ": " + virtualMachinePreoccupancy;
    }
}
