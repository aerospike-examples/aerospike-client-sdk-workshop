import React, { useState, useEffect } from "react";
import styles from "./index.module.css";
import clsx from "clsx";

const SizeOptions = ({options, onSizeChange}) => {
    const [selected, setSelected] = useState(0);

    useEffect(() => {
        // Initialize size on mount
        if (onSizeChange && options && options.length > 0 && options[0]) {
            onSizeChange(options[0].value);
        }
    }, []);

    const handleSizeChange = (idx) => {
        setSelected(idx);
        if (onSizeChange && options[idx]) {
            onSizeChange(options[idx].value);
        }
    };

    return (
        <div className={styles.options}>
            <h4>Size</h4>
            <div className={styles.sizeOptions}>
                {options.map((option, idx) => (
                    <div 
                        key={idx} 
                        className={clsx(styles.option, selected === idx && styles.selected)}
                        onClick={() => handleSizeChange(idx)}>
                        <span>{option.value}</span>
                    </div>
                ))}
            </div>
        </div>
    )
}

export default SizeOptions;