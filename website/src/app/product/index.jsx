import React from "react";
import styles from "./index.module.css";
import { useLoaderData } from "react-router-dom";
import Images from "../../components/images";
import Breadcrumbs from "../../components/breadcrumbs";
import ProductDetail from "../../components/productDetail";
import StyleOptions from "../../components/styleOptions";
import ProdDisplayHorizontal from "../../components/prodDisplayHorizontal";
import SizeOptions from "../../components/sizeOptions";
import { useCart } from "../../context/CartContext";
import { useToast } from "../../context/ToastContext";

export const productLoader = async (product) => {
    let response = await fetch(`http://localhost:8080/rest/v1/get?prod=${product}`);
    let { error, ...data } = await response.json();

    if(error) throw new Response("", {
        status: 404,
        statusText: "Not Found"
    });
    return data;
}

const Product = () => {
    const { product, related, also_bought } = useLoaderData();
    const { addToCart } = useCart();
    const { showSuccess, showError } = useToast();
    const [selectedSize, setSelectedSize] = React.useState(() => {
        // Initialize with first option value if available
        const initialSize = product?.options && product.options.length > 0 ? product.options[0].value : null;
        return initialSize;
    });

    const handleAddToCart = async (productId, quantity) => {
        const result = await addToCart(productId, quantity, selectedSize);
        if (result.success) {
            showSuccess(`${product.name} added to cart!`, 3000);
        } else {
            showError(`Failed to add item to cart: ${result.error}`, 3000);
        }
    };

    const handleSizeChange = (size) => {
        setSelectedSize(size);
    };

    return (
        <>
            <Breadcrumbs items={[
                product?.category,
                product?.subCategory,
                product?.usage,
                product?.name
            ]} />
            <div className={styles.product}>
                <div className={styles.productData}>
                    <div className={styles.options}>
                        <Images images={product.images} />
                        {product.styles && 
                        <StyleOptions options={product.styles} />}
                    </div>
                    <div className={styles.options}>
                        <ProductDetail 
                            name={product.name} 
                            descriptors={product?.descriptors} 
                            price={product?.price}
                            productId={product?.id}
                            onAddToCart={handleAddToCart}
                        />
                        {product.options && product.options.length > 0 &&
                        <SizeOptions options={product.options} onSizeChange={handleSizeChange} />}
                    </div>
                </div>
                {
                // <ProdDisplayHorizontal products={related} title="Similar items" />
                // <ProdDisplayHorizontal products={also_bought} title="Also bought" />
                }
            </div>
        </>
    )
}

export default Product;